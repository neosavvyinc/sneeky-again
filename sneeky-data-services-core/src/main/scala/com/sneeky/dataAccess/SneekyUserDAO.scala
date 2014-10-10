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

  def findByIdOperation(id : Long)(implicit session : Session) : Option[SneekyV2User] = {
    byIdQuery(id).firstOption
  }

  def updateLastAccessedOperation(user : SneekyV2User)(implicit session : Session) : Boolean = {
    val updateQuery = for { u <- UserTable if u.id === user.id } yield u.lastAccessed
    val numRows = updateQuery.update(Dates.nowDT)
    numRows > 0
  }

  def insertUser(user : SneekyV2User)(implicit session : Session) : SneekyV2User = {
    log.trace(s"inserting user: $user")
    val id = UserTable.forInsert.insert(user)
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

  def updateSetting(userId : Long, userSetting : SettingType, userValue : Boolean) : Boolean = {

    userSetting match {
      case NewMessagePushNotifications => db.withSession { implicit session =>
        val upQuery = for { u <- UserTable if u.id is userId } yield u.likeNotification
        val numRows = upQuery.update(userValue)
        numRows > 0
      }
      case _ => false
    }

  }

}

