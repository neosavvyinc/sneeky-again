package com.phantom.dataAccess

import scala.slick.session.Database
import spray.http.{ StatusCode, StatusCodes }
import org.joda.time.LocalDate
import com.phantom.ds.framework.Logging
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.model.{ PhantomUser, UserRegistration, UserLogin, Contact }
import scala.concurrent.{ ExecutionContext, Future, future }
import java.util.UUID

class DuplicateUserException extends Exception with PhantomException {
  val code = 101
}

class NonexistantUserException extends Exception with PhantomException {
  val code = 103
}

class PhantomUserDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db) with Logging {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  //move me
  def insert(user : PhantomUser) : PhantomUser = {
    log.trace(s"inserting user: $user")
    val id = UserTable.forInsert.insert(user)
    log.trace(s"id $id")
    user.copy(id = Some(id))
  }

  private val exists = for (email <- Parameters[String]; u <- UserTable if u.email is email) yield u.exists

  def register(registrationRequest : UserRegistration) : Future[PhantomUser] = {
    log.trace(s"registering $registrationRequest")
    future {
      log.trace("checking for existing user")
      val ex = exists(registrationRequest.email).firstOption
      log.trace(s"results $ex")

      val mapped = ex.map { e =>
        log.trace("inside mapppppinggg")
        if (e) {
          log.trace(s"duplicate email detected when registering $registrationRequest")
          throw new DuplicateUserException
        } else {
          createUserRecord(registrationRequest)
        }
      }

      mapped.getOrElse(createUserRecord(registrationRequest))

    }
  }

  private def createUserRecord(reg : UserRegistration) = {
    log.trace("creating user record")
    val uuid = UUID.randomUUID()
    insert(PhantomUser(None, uuid, reg.email, reg.birthday, true, ""))
  }

  def login(loginRequest : UserLogin) : Future[PhantomUser] = {
    //log.info(s"logging in $loginRequest")

    Query(UserTable).filter(_.email is loginRequest.email)
      .firstOption.map { u : PhantomUser =>
        // check hashed password vs. loginRequest.password
        Future.successful(
          u
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

  def updateContacts(id : Long, contacts : String) : Future[StatusCode] = {

    import scala.slick.jdbc.StaticQuery
    import java.lang.StringBuilder
    //val q = Query(ContactTable).filter(_.ownerId is id)

    //val users = Query(UserTable).filter(_.phoneNumber === contacts)
    //select x2.`id`, x2.`EMAIL`, x2.`BIRTHDAY`, x2.`ACTIVE`, x2.`PHONE_NUMBER` from `USERS` x2 where x2.`PHONE_NUMBER` = '\"6148551499\"'

    // oh man, this is dumb...
    var sb = new StringBuilder
    sb.append("""select x2.`id` from `USERS` x2 where x2.`PHONE_NUMBER` = '""")
    sb.append(contacts)
    sb.append("""'""")
    val q = sb.toString.replace(""""""", """""")

    val users = StaticQuery.queryNA[Long](q)

    users.list.foreach { uid : Long =>
      ContactTable.insert(Contact(None, id, uid, "friend"))
    }

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
        PhantomUser(None, UUID.randomUUID, "chris@test.com", new LocalDate(2003, 12, 21), true, "6144993676"),
        PhantomUser(None, UUID.randomUUID, "adam@test.com", new LocalDate(2003, 12, 21), true, "6141234567"),
        PhantomUser(None, UUID.randomUUID, "trevor@test.com", new LocalDate(2003, 12, 21), true, "6148911787"),
        PhantomUser(None, UUID.randomUUID, "bob@test.com", new LocalDate(2003, 12, 21), true, "6148551499")
      )

      // uncomment this, the transaction will fail and no users
      // will be inserted
      //val dumbComputation : Int = 1 / 0
    }

  }
}

