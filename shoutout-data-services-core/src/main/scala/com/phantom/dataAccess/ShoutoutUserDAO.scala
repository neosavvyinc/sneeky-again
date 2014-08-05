package com.phantom.dataAccess

import org.joda.time.LocalDate

import scala.slick.session.Database
import com.phantom.ds.framework.Logging
import com.phantom.ds.framework.exception.ShoutoutException
import com.phantom.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import java.util.UUID
import com.phantom.ds.user.Passwords

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

  private val existsQuery = for (
    email <- Parameters[String];
    u <- UserTable if u.email.toLowerCase is email.toLowerCase
  ) yield u.exists

  private def findByEmailOperation(email : String)(implicit session : Session) : Option[ShoutoutUser] = {
    byEmailQuery(email.toLowerCase).firstOption
  }

  private def findByFacebookOperation(email : String)(implicit session : Session) : Option[ShoutoutUser] = {
    byFacebookQuery(email.toLowerCase).firstOption
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
      reg.email.toLowerCase))
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

  def loginByFacebook(loginRequest : FacebookUserLogin) : Future[ShoutoutUser] = {
    future {
      db.withSession { implicit session =>
        log.trace(s"logging in $loginRequest")
        val userOpt = for {
          user <- findByFacebookOperation(loginRequest.facebookId)
        } yield user

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
          "")))

        user
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

}

