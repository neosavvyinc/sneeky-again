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
    future {
      Query(UserTable).filter(_.id is id)
        .firstOption.map { u : PhantomUser => u }
        .getOrElse(throw PhantomException.nonExistentUser)
    }
  }

  def findByPhoneNumbers(phoneNumbers : Set[String]) : Future[List[PhantomUser]] = {
    future {
      val q = for { u <- UserTable if u.phoneNumber inSet phoneNumbers } yield u
      q.list
    }
  }

  def findContacts(id : Long) : Future[List[PhantomUser]] = {
    future {
      val q = for {
        u <- UserTable
        c <- ContactTable if u.id === c.contactId && c.ownerId === id
      } yield u

      q.list match {
        case u : List[PhantomUser] => u
        case _                     => throw PhantomException.nonExistentContact
      }
    }
  }

  def blockedContactsQuery(id : Long) = {
    for {
      c <- ContactTable if c.contactType === "block" && c.ownerId === id
    } yield c.contactType
  }

  def findPhantomUserIdsByPhone(contacts : List[String]) : Future[List[Long]] = {
    future {
      val q = for {
        u <- UserTable if u.phoneNumber inSet contacts
      } yield u

      q.list.map(_.id.get)
    }
  }

  def clearBlockList(id : Long) : Future[StatusCode] = {
    future {
      blockedContactsQuery(id).update("friend") match {
        case 0 => StatusCodes.NotModified
        case _ => StatusCodes.OK
      }
    }

  }
}

