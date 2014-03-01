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

class PhantomUserDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  //ONLY USED BY TESTS
  def insert(user : PhantomUser) : PhantomUser = {
    db.withTransaction { implicit session =>
      insertNoTransact(user)
    }
  }

  def insertAllOperation(users : Seq[PhantomUser])(implicit session : Session) : Seq[PhantomUser] = {
    val b = UserTable.forInsert.insertAll(users : _*)
    b.zip(users).map {
      case (id, user) =>
        user.copy(id = Some(id))
    }
  }

  private def insertNoTransact(user : PhantomUser)(implicit session : Session) : PhantomUser = {
    log.trace(s"inserting user: $user")
    val id = UserTable.forInsert.insert(user.copy(email = user.email.map(_.toLowerCase)))
    log.trace(s"id $id")
    user.copy(id = Some(id))
  }

  private val byEmailQuery = for (email <- Parameters[String]; u <- UserTable if u.email.toLowerCase is email.toLowerCase) yield u

  private val existsQuery = for (email <- Parameters[String]; u <- UserTable if u.email.toLowerCase is email.toLowerCase) yield u.exists

  def registerOperation(registrationRequest : UserRegistration)(implicit session : Session) : PhantomUser = {
    log.trace(s"registering $registrationRequest")
    val ex = existsQuery(registrationRequest.email.toLowerCase).firstOption
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

  private def createUserRecord(reg : UserRegistration)(implicit session : Session) = {
    insertNoTransact(PhantomUser(None, UUID.randomUUID, Some(reg.email.toLowerCase), Some(Passwords.getSaltedHash(reg.password)), Some(reg.birthday), true, None))
  }

  def login(loginRequest : UserLogin) : Future[PhantomUser] = {
    future {
      db.withSession { implicit session =>
        log.trace(s"logging in $loginRequest")
        val userOpt = for {
          user <- findByEmailOperation(loginRequest.email)
          password <- user.password if Passwords.check(loginRequest.password, password)
        } yield user

        val user = userOpt.getOrElse(throw PhantomException.nonExistentUser)
        user
      }
    }
  }

  def verifyUserOperation(uuid : UUID, phoneNumber : String)(implicit session : Session) : Option[Long] = {
    //inefficient
    val uOpt = (for { u <- UserTable if u.uuid === uuid && u.status === (Unverified : UserStatus) } yield u).firstOption
    uOpt.foreach { phantomUser =>
      val upQuery = for { u <- UserTable if u.id is phantomUser.id.get } yield u.status ~ u.phoneNumber
      upQuery.update(Verified, phoneNumber)
    }
    if (uOpt.isEmpty) {
      log.warn(s"$uuid does not match any unverified users")
    }
    uOpt.map(_.id.get)
  }

  private val stubUserByPhoneNumber = for {
    phoneNumber <- Parameters[String]
    u <- UserTable if u.phoneNumber === phoneNumber && u.status === (Stub : UserStatus)
  } yield u

  def findMatchingStubUserOperation(number : String)(implicit session : Session) : Option[PhantomUser] = {
    stubUserByPhoneNumber(number).firstOption
  }

  private val findById = for { id <- Parameters[Long]; u <- UserTable if u.id === id } yield u

  def find(id : Long) : Option[PhantomUser] = {
    db.withSession { implicit session =>
      findById(id).firstOption
    }
  }

  private val findUserByUUID = for { uuid <- Parameters[UUID]; u <- UserTable if u.uuid === uuid } yield u

  def findByUUID(uuid : UUID) : Option[PhantomUser] = {
    db.withSession { implicit session =>
      findUserByUUID(uuid).firstOption
    }
  }

  def findByEmailOperation(email : String)(implicit session : Session) : Option[PhantomUser] = {
    byEmailQuery(email.toLowerCase).firstOption
  }

  def findByPhoneNumbers(phoneNumbers : Set[String]) : Future[List[PhantomUser]] = {
    future {
      db.withSession { implicit session =>
        val q = for { u <- UserTable if u.phoneNumber inSet phoneNumbers } yield u
        q.list
      }
    }
  }

  //TODO: future me (or remove..used by dead code only)
  def findContacts(id : Long) : Future[List[PhantomUser]] = {
    db.withSession { implicit session =>
      val q = for {
        u <- UserTable
        c <- ContactTable if u.id === c.contactId && c.ownerId === id
      } yield u

      q.list match {
        case u : List[PhantomUser] => Future.successful(u)
        case _                     => Future.failed(new Exception())
      }
    }
  }

  def findPhantomUserIdsByPhone(contacts : List[String]) : (List[PhantomUser], List[String]) = {
    db.withSession { implicit session =>
      val q = for {
        u <- UserTable if (u.status === (Verified : UserStatus)) && (u.phoneNumber inSet contacts)
      } yield u

      val users = q.list
      val userNumbers = users.map(_.phoneNumber).flatten

      // note, checking on Verified above, so this will include unverified and stub users in
      // addition to not found numbers. I suppose this is okay for now, as we're not actually
      // using notFound for anything at the moment
      val notFound = contacts.diff(userNumbers)
      (users, notFound)
    }
  }

  //how in the hell do i exec update blank from users set invite = invite +1 where id in(..) ?
  def updateInvitationCount(users : Seq[PhantomUser]) : Future[Int] = {
    future {
      db.withTransaction { implicit session =>
        val ids = users.map(_.id.get).mkString(",")
        val statement = session.conn.createStatement()
        val q = s"update USERS set invitation_count = invitation_count + 1 where id in ($ids)"
        statement.executeUpdate(q)
      }
    }
  }

  def clearBlockListOperation(id : Long)(implicit session : Session) : Int = {
    val q = for { c <- ContactTable if c.ownerId === id && c.contactType === (Blocked : ContactType) } yield c.contactType
    q.update(Friend)
  }

  def deleteOperation(id : Long)(implicit session : Session) : Int = {
    val q = for { u <- UserTable if u.id === id } yield u
    q.delete
  }

  def updateSetting(userId : Long, userSetting : SettingType, userValue : Boolean) : Boolean = {

    userSetting match {
      case SoundOnNewNotification => db.withSession { implicit session =>
        val upQuery = for { u <- UserTable if u.id is userId } yield u.settingSound
        val numRows = upQuery.update(userValue)
        numRows > 0
      }
      case NotificationOnNewPicture => db.withSession { implicit session =>
        val upQuery = for { u <- UserTable if u.id is userId } yield u.settingNewPicture
        val numRows = upQuery.update(userValue)
        numRows > 0
      }
      case MutualContactMessaging => db.withSession { implicit session =>
        val upQuery = for { u <- UserTable if u.id is userId } yield u.mutualContactSetting
        val numRows = upQuery.update(userValue)
        numRows > 0
      }
      case _ => false
    }

  }

  def updatePasswordForUserOperation(email : String, newPassword : String)(implicit session : Session) : Boolean = {
    val updateQuery = for { u <- UserTable if u.email === email.toLowerCase } yield u.password
    val numRows = updateQuery.update(newPassword)
    numRows > 0
  }

}

