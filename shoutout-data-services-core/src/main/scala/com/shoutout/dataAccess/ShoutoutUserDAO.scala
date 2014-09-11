package com.shoutout.dataAccess

import org.joda.time.LocalDate

import scala.slick.session.Database
import com.shoutout.ds.framework.{ Dates, Logging }
import com.shoutout.ds.framework.exception.ShoutoutException
import com.shoutout.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import java.util.UUID
import com.shoutout.ds.user.Passwords

class ShoutoutUserDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
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

  def findByEmailOperation(email : String)(implicit session : Session) : Option[ShoutoutUser] = {
    byEmailQuery(email.toLowerCase).firstOption
  }

  def findByUsernameOperation(username : String)(implicit session : Session) : Option[ShoutoutUser] = {
    byUsernameQuery(username).firstOption
  }

  def findByFacebookIds(facebookIds : List[String])(implicit session : Session) : List[ShoutoutUser] = {
    val fbQuery = for {
      u <- UserTable if u.facebookID inSet facebookIds
    } yield u

    fbQuery.list
  }

  def findByFacebookOperation(email : String)(implicit session : Session) : Option[ShoutoutUser] = {
    byFacebookQuery(email.toLowerCase).firstOption
  }

  def findByIdOperation(id : Long)(implicit session : Session) : Option[ShoutoutUser] = {
    byIdQuery(id).firstOption
  }

  def updatePasswordForUserOperation(email : String, newPassword : String)(implicit session : Session) : Boolean = {
    val updateQuery = for { u <- UserTable if u.email === email.toLowerCase } yield u.password
    val numRows = updateQuery.update(newPassword)
    numRows > 0
  }

  def updateLastAccessedOperation(user : ShoutoutUser)(implicit session : Session) : Boolean = {
    val updateQuery = for { u <- UserTable if u.id === user.id } yield u.lastAccessed
    val numRows = updateQuery.update(Dates.nowDT)
    numRows > 0
  }

  def updatePasswordForUserOperation(id : Long, newPassword : String)(implicit session : Session) : Boolean = {
    val updateQuery = for { u <- UserTable if u.id === id } yield u.password
    val numRows = updateQuery.update(newPassword)
    numRows > 0
  }

  private def insertNoTransact(user : ShoutoutUser)(implicit session : Session) : ShoutoutUser = {
    log.trace(s"inserting user: $user")
    val id = UserTable.forInsert.insert(user.copy(email = user.email.map(_.toLowerCase)))
    log.trace(s"id $id")
    user.copy(id = Some(id))
  }

  private def createUserRecord(reg : UserRegistrationRequest)(implicit session : Session) = {
    insertNoTransact(ShoutoutUser(
      None,
      UUID.randomUUID,
      None,
      Some(reg.email.toLowerCase),
      Some(Passwords.getSaltedHash(reg.password)),
      None,
      None,
      None,
      "",
      None))
  }

  /**
   * ***********************************************************
   * External DAO API
   * ***********************************************************
   */

  def login(loginRequest : UserLogin) : Future[ShoutoutUser] = {
    future {
      db.withSession { implicit session =>
        log.trace(s"logging in $loginRequest")
        val userOpt = for {
          user <- findByEmailOperation(loginRequest.email)
          password <- user.password if Passwords.check(loginRequest.password, password)
        } yield user

        val user = userOpt.getOrElse(throw ShoutoutException.nonExistentUser)
        user
      }
    }
  }

  def loginByFacebook(loginRequest : FacebookUserLogin) : Future[(ShoutoutUser, Boolean)] = {
    future {
      db.withSession { implicit session =>
        var userExists = false;

        log.trace(s"logging in $loginRequest")
        val userOpt = for {
          user <- findByFacebookOperation(loginRequest.facebookId)
        } yield user

        userOpt match {
          case Some(_) => userExists = true
          case None    => userExists = false
        }

        val user = userOpt.getOrElse(insertNoTransact(ShoutoutUser(
          None,
          UUID.randomUUID,
          Some(loginRequest.facebookId),
          None,
          None,
          loginRequest.birthdate match {
            case Some(x) => Some(x)
            case None    => None
          },
          loginRequest.firstName match {
            case Some(x) => Some(x)
            case None    => None
          },
          loginRequest.lastName match {
            case Some(x) => Some(x)
            case None    => None
          },
          "",
          None)))

        (user, userExists)
      }
    }
  }

  def registerOperation(registrationRequest : UserRegistrationRequest)(implicit session : Session) : ShoutoutUser = {
    log.trace(s"registering $registrationRequest")
    val ex = existsQuery(registrationRequest.email.toLowerCase).firstOption
    val mapped = ex.map { e =>
      if (e) {
        log.trace(s"duplicate email detected when registering $registrationRequest")
        throw ShoutoutException.duplicateUser
      } else {
        createUserRecord(registrationRequest)
      }
    }
    mapped.getOrElse(createUserRecord(registrationRequest))
  }

  def findById(id : Long) : Future[ShoutoutUser] = {
    future {
      db.withSession { implicit session =>
        val userOpt = for {
          user <- findByIdOperation(id)
        } yield user

        userOpt.getOrElse(throw ShoutoutException.nonExistentUser)
      }
    }
  }

  def findByIds(ids : Set[Long]) : List[ShoutoutUser] = {
    db.withSession { implicit session =>
      val q = for {
        u <- UserTable if u.id inSet ids
      } yield u

      q.list
    }
  }

  def update(persistentUser : ShoutoutUser, updateRequest : ShoutoutUserUpdateRequest) : Future[Int] = {
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

        val updated = ShoutoutUser(
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

  def updateProfilePicUrl(updatedUser : ShoutoutUser) : Int = {
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

  def isUserNameRestricted(username : String) : Boolean = {
    db.withSession { implicit session =>
      val q = for { restriction <- RestrictionTable if restriction.restrictedName === username } yield restriction
      q.firstOption match {
        case Some(r) => true
        case None    => false
      }
    }
  }
}

