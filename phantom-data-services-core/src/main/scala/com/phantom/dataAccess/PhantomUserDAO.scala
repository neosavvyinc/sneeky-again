package com.phantom.dataAccess

import scala.slick.session.Database
import spray.http.{ StatusCode, StatusCodes }
import org.joda.time.LocalDate
import com.phantom.ds.framework.Logging
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.model.{ PhantomUser, UserRegistration, ClientSafeUserResponse, UserLogin, Contact }
import scala.concurrent.{ ExecutionContext, Future }

class DuplicateUserException extends Exception with PhantomException {
  val code = 101
}

class NonexistantUserException extends Exception with PhantomException {
  val code = 103
}

class PhantomUserDAO(dal : DataAccessLayer, db : Database) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._
  import com.phantom.model.PhantomUserTypes._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def insert(user : PhantomUser) : PhantomUser = {
    val id = UserTable.forInsert.insert(user)
    PhantomUser(Some(id), user.email, user.birthday, user.active, user.phoneNumber)
  }

  def register(registrationRequest : UserRegistration) : Future[PhantomUser] = {
    //log.info(s"registering $registrationRequest")

    Query(UserTable).filter(_.email is registrationRequest.email)
      .firstOption.map { u : PhantomUser =>
        Future.failed(new DuplicateUserException())
      }.getOrElse {
        //
        // confirm / enter confirmation code service ?
        //
        Future.successful {
          insert(PhantomUser(None, registrationRequest.email, registrationRequest.birthday, true, ""))
        }
      }
  }

  def login(loginRequest : UserLogin) : Future[PhantomUser] = {
    //log.info(s"logging in $loginRequest")

    Query(UserTable).filter(_.email is loginRequest.email)
      .firstOption.map { u : PhantomUser =>
        // check hashed password vs. loginRequest.password
        Future.successful(
          PhantomUser(None, u.email, u.birthday, true, u.phoneNumber)
        )
        //case _ => 
      }.getOrElse {
        Future.failed(new NonexistantUserException())
      }
  }

  def find(id : Long) : Future[PhantomUser] = {
    //log.info(s"finding contacts for user with id => $id")
    Query(UserTable).filter(_.id is id)
      .firstOption.map { u : PhantomUser => Future.successful(u) }
      .getOrElse(Future.failed(new NonexistantUserException()))
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

  def updateContacts(id : Long, contacts : List[PhoneNumber]) : Future[StatusCode] = {

    // in transaction?
    val q = Query(ContactTable).filter(_.ownerId is id)

    val contactIds = for {
      c <- ContactTable if c.ownerId === id
    } yield c.contactId

    Future.successful(StatusCodes.OK)
  }

  def clearBlockList(id : Long) : Future[StatusCode] = {

    findBlockedContacts(id).update("friend") match {
      case 0 => Future.successful(StatusCodes.NotModified)
      case _ => Future.successful(StatusCodes.OK)
    }
  }

  def createSampleUsers = {

    implicitSession.withTransaction {
      println("in a transaction...")

      UserTable.insertAll(
        PhantomUser(None, "chris@test.com", new LocalDate(2003, 12, 21), true, "1234567"),
        PhantomUser(None, "adam@test.com", new LocalDate(2003, 12, 21), true, "1234567"),
        PhantomUser(None, "trevor@test.com", new LocalDate(2003, 12, 21), true, "1234567")
      )

      // uncomment this, the transaction will fail and no users
      // will be inserted
      // val dumbComputation : Int = 1 / 0
    }

  }
}

