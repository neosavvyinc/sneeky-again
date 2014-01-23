package com.phantom.dataAccess

import scala.slick.session.Database
import spray.http.{ StatusCode, StatusCodes }
import com.phantom.ds.framework.Logging
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import java.util.UUID
import com.phantom.ds.user.Passwords
import com.phantom.model.UserLogin
import com.phantom.model.PhantomUser
import com.phantom.model.UserRegistration
import com.phantom.model.Contact

class PhantomUserDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  def insert(user : PhantomUser) : PhantomUser = {
    log.trace(s"inserting user: $user")

    val id = UserTable.forInsert.insert(user)
    log.trace(s"id $id")
    user.copy(id = Some(id))
  }

  private val byEmailQuery = for (email <- Parameters[String]; u <- UserTable if u.email is email) yield u

  private val existsQuery = for (email <- Parameters[String]; u <- UserTable if u.email is email) yield u.exists

  def register(registrationRequest : UserRegistration) : Future[PhantomUser] = {
    log.trace(s"registering $registrationRequest")
    future {
      log.trace("checking for existing user")
      val ex = existsQuery(registrationRequest.email).firstOption
      val mapped = ex.map { e =>
        if (e) {
          log.trace(s"duplicate email detected when registering $registrationRequest")
          throw PhantomException.duplicateUser
        } else {
          createUserRecord(registrationRequest)
        }
      }
      mapped.getOrElse(createUserRecord(registrationRequest))
    }
  }

  private def createUserRecord(reg : UserRegistration) = {
    insert(PhantomUser(None, UUID.randomUUID, reg.email, Passwords.getSaltedHash(reg.password), reg.birthday, true, ""))
  }

  def login(loginRequest : UserLogin) : Future[PhantomUser] = {
    future {
      log.trace(s"logging in $loginRequest")
      val userOpt = byEmailQuery(loginRequest.email).firstOption
      val filtered = userOpt.filter(x => Passwords.check(loginRequest.password, x.password))
      val user = filtered.getOrElse(throw PhantomException.nonExistentUser)
      if (user.status == Unverified) {
        throw PhantomException.unverifiedUser(user.uuid.toString)
      }
      user
    }
  }

  def verifyUser(uuid : UUID) : Future[Int] = {
    future {
      //inefficient
      val q = for { u <- UserTable if u.uuid === uuid && u.status === (Unverified : UserStatus) } yield u.status
      q.update(Verified)
    }
  }

  def find(id : Long) : Future[PhantomUser] = {
    //log.info(s"finding contacts for user with id => $id")
    Query(UserTable).filter(_.id is id)
      .firstOption.map { u : PhantomUser => Future.successful(u) }
      .getOrElse(Future.failed(PhantomException.nonExistentUser))
  }

  def findContacts(id : Long) : Future[List[PhantomUser]] = {
    val q = for {
      u <- UserTable
      c <- ContactTable if u.id === c.contactId && c.ownerId === id
    } yield u

    q.list match {
      case u : List[PhantomUser] => Future.successful(u)
      case _                     => Future.failed(new Exception())
    }
  }

  def findBlockedContacts(id : Long) = {
    for {
      c <- ContactTable if c.contactType === "block" && c.ownerId === id
    } yield c.contactType
  }

  def findContactIdsByPhone(id : Long, contacts : List[String]) : Future[List[Long]] = {

    future {

      val users = for {
        c <- ContactTable if c.ownerId is id
        u <- UserTable if u.id === c.contactId
      } yield u
      users.list.filter(u => contacts.contains(u.phoneNumber)).map(_.id.get)
    }
  }

  def clearBlockList(id : Long) : Future[StatusCode] = {

    findBlockedContacts(id).update("friend") match {
      case 0 => Future.successful(StatusCodes.NotModified)
      case _ => Future.successful(StatusCodes.OK)
    }
  }
}

