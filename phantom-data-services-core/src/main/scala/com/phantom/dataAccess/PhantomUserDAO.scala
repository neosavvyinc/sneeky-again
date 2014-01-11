package com.phantom.dataAccess

import scala.slick.session.Database
import spray.http.{ StatusCode, StatusCodes }
import org.joda.time.LocalDate
import com.phantom.ds.framework.Logging
import com.phantom.model.{ PhantomUser, UserRegistration, ClientSafeUserResponse, UserLogin, Contact }
import scala.concurrent.{ ExecutionContext, Future }

class PhantomUserDAO(dal : DataAccessLayer, db : Database) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

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
        Future.failed(new Exception())
      }.getOrElse {
        //
        // confirm / enter confirmation code service ?
        //
        Future.successful {
          insert(PhantomUser(None, registrationRequest.email, new LocalDate(12345678), true, "6148551499"))
        }
      }
  }

  def login(loginRequest : UserLogin) : Future[ClientSafeUserResponse] = {
    //log.info(s"logging in $loginRequest")

    Query(UserTable).filter(_.email is loginRequest.email)
      .firstOption.map { u : PhantomUser =>
        // check hashed password vs. loginRequest.password
        Future.successful(
          ClientSafeUserResponse(u.email, "6148551499", "1234", false, false)
        )
        //case _ => 
      }.getOrElse {
        Future.failed(new Exception())
      }
  }

  def findById(id : Long) : Future[ClientSafeUserResponse] = {
    //log.info(s"finding contacts for user with id => $id")
    Query(UserTable).filter(_.id is id)
      .firstOption.map { u : PhantomUser => Future.successful(ClientSafeUserResponse(u.email, "6144993676", "1234", false, false)) }
      .getOrElse(Future.failed(new Exception()))
  }

  def findContactsById(id : Long) : Future[List[PhantomUser]] = {
    val q = for {
      u <- UserTable
      c <- ContactTable if u.id === c.contactId && c.ownerId === id
    } yield u

    q.list match {
      case u : List[PhantomUser] => Future.successful(u)
      case _                     => Future.failed(new Exception())
    }
  }

  def findContactsByPhone : Unit = {

  }

  def updateContacts(id : Long, contacts : List[String]) : Future[StatusCode] = {

    // in transaction?
    val q = Query(ContactTable).filter(_.ownerId is id)

    val contactIds = for {
      c <- ContactTable if c.ownerId === id
    } yield c.contactId

    Future.successful(StatusCodes.OK)
  }

  def clearBlockList(id : Long) : Future[StatusCode] = {

    val q = for {
      c <- ContactTable if c.contactType === "block" && c.ownerId === id
    } yield c.contactType
    q.update("friend")

    // error handling / how to return something else if update fails?
    Future.successful(StatusCodes.OK)
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

