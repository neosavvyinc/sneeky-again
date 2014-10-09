package com.sneeky.dataAccess

import org.joda.time.LocalDate

import scala.slick.session.Database
import com.sneeky.ds.framework.{ Dates, Logging }
import com.sneeky.ds.framework.exception.ShoutoutException
import com.sneeky.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import java.util.UUID
import com.sneeky.ds.user.Passwords

class SneekyUserDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  /**
   * ***********************************************************
   * Internal DAO API
   * ***********************************************************
   */
  private val byEmailQuery = for (
    email <- Parameters[String];
    u <- UserTable if u.email.toLowerCase is email.toLowerCase
  ) yield u

  private val byFacebookQuery = for (
    facebookId <- Parameters[String];
    u <- UserTable if u.facebookID is facebookId
  ) yield u

  private val byUsernameQuery = for (
    username <- Parameters[String];
    u <- UserTable if u.username is username
  ) yield u
  //
  //  private val byFacebookIdsQuery = for {
  //    ids <- Parameters[Set[String]]
  //    u <- UserTable if u.facebookID inSet ids
  //  } yield u

  private val existsQuery = for (
    email <- Parameters[String];
    u <- UserTable if u.email.toLowerCase is email.toLowerCase
  ) yield u.exists

  private val byIdQuery = for {
    id <- Parameters[Long];
    u <- UserTable if u.id === id
  } yield u

  def userExistsOperation(id : Long)(implicit session : Session) : Option[Boolean] = {
    val userExistsQuery = for {
      u <- UserTable if u.id === id
    } yield u.exists
    userExistsQuery.firstOption
  }

  def findByEmailOperation(email : String)(implicit session : Session) : Option[SneekyV2User] = {
    byEmailQuery(email.toLowerCase).firstOption
  }

  def findByUsernameOperation(username : String)(implicit session : Session) : Option[SneekyV2User] = {
    byUsernameQuery(username).firstOption
  }

  def findByFacebookIds(facebookIds : List[String])(implicit session : Session) : List[SneekyV2User] = {
    val fbQuery = for {
      u <- UserTable if u.facebookID inSet facebookIds
    } yield u

    fbQuery.list
  }

  def findByFacebookOperation(email : String)(implicit session : Session) : Option[SneekyV2User] = {
    byFacebookQuery(email.toLowerCase).firstOption
  }

  def findByIdOperation(id : Long)(implicit session : Session) : Option[SneekyV2User] = {
    byIdQuery(id).firstOption
  }

  def updatePasswordForUserOperation(email : String, newPassword : String)(implicit session : Session) : Boolean = {
    val updateQuery = for { u <- UserTable if u.email === email.toLowerCase } yield u.password
    val numRows = updateQuery.update(newPassword)
    numRows > 0
  }

  def updateLastAccessedOperation(user : SneekyV2User)(implicit session : Session) : Boolean = {
    val updateQuery = for { u <- UserTable if u.id === user.id } yield u.lastAccessed
    val numRows = updateQuery.update(Dates.nowDT)
    numRows > 0
  }

  def updatePasswordForUserOperation(id : Long, newPassword : String)(implicit session : Session) : Boolean = {
    val updateQuery = for { u <- UserTable if u.id === id } yield u.password
    val numRows = updateQuery.update(newPassword)
    numRows > 0
  }

  private def insertNoTransact(user : SneekyV2User)(implicit session : Session) : SneekyV2User = {
    log.trace(s"inserting user: $user")
    val id = UserTable.forInsert.insert(user.copy(email = user.email.map(_.toLowerCase)))
    log.trace(s"id $id")
    user.copy(id = Some(id))
  }

  /**
   * ***********************************************************
   * External DAO API
   * ***********************************************************
   */

  def findById(id : Long) : Future[SneekyV2User] = {
    future {
      db.withSession { implicit session =>
        val userOpt = for {
          user <- findByIdOperation(id)
        } yield user

        userOpt.getOrElse(throw ShoutoutException.nonExistentUser)
      }
    }
  }

  def findByIds(ids : Set[Long]) : List[SneekyV2User] = {
    db.withSession { implicit session =>
      val q = for {
        u <- UserTable if u.id inSet ids
      } yield u

      q.list
    }
  }

  def findAll()(implicit session : Session) : List[SneekyV2User] = {
    val q = for { u <- UserTable } yield u
    q.list()
  }

  private val usersByLocale = for {
    locale <- Parameters[String]
    (s, u) <- SessionTable innerJoin UserTable on ((sess, user) => sess.userId === user.id && sess.locale === locale)
  } yield u

  def findAllEnglish()(implicit session : Session) : List[SneekyV2User] = {
    val q = for {
      locale <- Parameters[String]
      (s, u) <- SessionTable innerJoin UserTable on ((sess, user) => sess.userId === user.id && (sess.locale === locale || sess.locale === null.asInstanceOf[String]))
    } yield u
    q("en_US").list()
  }

  def findAllForLocale(locale : String)(implicit session : Session) : List[SneekyV2User] = {
    usersByLocale(locale).list()
  }

  def update(persistentUser : SneekyV2User, updateRequest : ShoutoutUserUpdateRequest) : Future[Int] = {
    future {
      db.withSession { implicit session =>

        updateRequest.username match {
          case Some(x) => {
            val userNameExists = for { user <- UserTable if user.username.toLowerCase === updateRequest.username.get.toLowerCase } yield user
            val userFromUsername = userNameExists.list()

            //only throw the exception if the username isn't yours
            if (userFromUsername.length > 0 && userFromUsername(0).id != persistentUser.id) {
              throw ShoutoutException.usernameNotAvailable
            }
          }
          case _ => //nothing happens
        }

        val updated = SneekyV2User(
          persistentUser.id,
          persistentUser.uuid,
          persistentUser.facebookID,
          persistentUser.email,
          persistentUser.password,
          updateRequest.birthday match {
            case None    => persistentUser.birthday
            case Some(x) => updateRequest.birthday
          },
          updateRequest.firstName match {
            case None    => persistentUser.firstName
            case Some(x) => updateRequest.firstName
          },
          updateRequest.lastName match {
            case None    => persistentUser.lastName
            case Some(x) => updateRequest.lastName
          },
          updateRequest.username.getOrElse(persistentUser.username),
          persistentUser.profilePictureUrl,
          persistentUser.newMessagePush
        )

        val q = for { user <- UserTable if user.id === persistentUser.id } yield user
        val count : Int = q.update(updated)

        if (count > 0)
          count
        else
          throw ShoutoutException.userNotUpdated

      }

    }
  }

  def updateProfilePicUrl(updatedUser : SneekyV2User) : Int = {
    db.withSession { implicit session =>
      val q = for { user <- UserTable if user.id === updatedUser.id } yield user
      q.update(updatedUser)
    }
  }

  def updateSetting(userId : Long, userSetting : SettingType, userValue : Boolean) : Boolean = {

    userSetting match {
      case NewMessagePushNotifications => db.withSession { implicit session =>
        val upQuery = for { u <- UserTable if u.id is userId } yield u.newMessagePush
        val numRows = upQuery.update(userValue)
        numRows > 0
      }
      case _ => false
    }

  }

}

