package com.phantom.model

import com.phantom.dataAccess.Profile
import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import scala.slick.lifted.ColumnOption.DBType
import java.util.UUID

case class UserRegistration(email : String,
                            birthday : LocalDate,
                            password : String)

case class RegistrationResponse(verificationUUID : UUID, sessionUUID : UUID)

case class RegistrationVerification(messageSid : String,
                                    accountSid : String,
                                    from : String,
                                    to : String,
                                    body : String,
                                    numMedia : Int)

case class UserLogin(email : String,
                     password : String)

case class LoginSuccess(sessionUUID : UUID)

case class PhantomUserDeleteMe(id : String)

case class SessionIDWithPushNotifier(sessionUUID : UUID,
                                     pushNotifier : String)

sealed trait UserStatus

object UserStatus {
  def toStringRep(status : UserStatus) : String = status match {
    case Unverified => "unverified"
    case Verified   => "verified"
  }

  def fromStringRep(str : String) : UserStatus = str.toLowerCase match {
    case "unverified" => Unverified
    case "verified"   => Verified
    case x            => throw new Exception(s"unrecognized user status %x")
  }
}

case object Unverified extends UserStatus

case object Verified extends UserStatus

object UUIDConversions {

  val toStringRep : (UUID => String) = _.toString

  val fromStringRep = UUID.fromString _

}

trait Phantom {
  def phoneNumber : String
}

case class PhantomUser(id : Option[Long],
                       uuid : UUID,
                       email : String,
                       password : String,
                       birthday : LocalDate,
                       active : Boolean,
                       phoneNumber : String,
                       status : UserStatus = Unverified) extends Phantom

object PhantomSession {

  def newSession(user : PhantomUser) : PhantomSession = {
    val now = DateTime.now(DateTimeZone.UTC)
    PhantomSession(UUID.randomUUID(), user.id.getOrElse(-1), now, now, None)
  }
}

case class PhantomSession(sessionId : UUID, userId : Long, created : DateTime, lastAccessed : DateTime, applePushID : Option[String] = None)

trait UserComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val UserStatusMapper = MappedTypeMapper.base[UserStatus, String](UserStatus.toStringRep, UserStatus.fromStringRep)

  implicit val UUIDMapper = MappedTypeMapper.base[UUID, String](UUIDConversions.toStringRep, UUIDConversions.fromStringRep)

  object UserTable extends Table[PhantomUser]("USERS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def uuid = column[UUID]("UUID")
    def email = column[String]("EMAIL", DBType("VARCHAR(256)"))
    def password = column[String]("PASSWORD", DBType("VARCHAR(300)"))
    def birthday = column[LocalDate]("BIRTHDAY")
    def active = column[Boolean]("ACTIVE")
    def phoneNumber = column[String]("PHONE_NUMBER")
    def status = column[UserStatus]("STATUS")
    def appleNoteSound = column[String]("SOUND_NOTIF")
    def appleNoteNewPicture = column[String]("NEW_PICTURE_NOTIF")

    def * = id.? ~ uuid ~ email ~ password ~ birthday ~ active ~ phoneNumber ~ status <> (PhantomUser, PhantomUser.unapply _)
    def forInsert = * returning id
    //    def phoneUnique = index("phoneUnique", phoneNumber, unique = true)

  }
}

trait UserSessionComponent { this : Profile with UserComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  object SessionTable extends Table[PhantomSession]("SESSIONS") {
    def sessionId = column[UUID]("SESSIONID")
    def userId = column[Long]("USERID")
    def created = column[DateTime]("CREATED")
    def lastAccessed = column[DateTime]("LASTACCESSED")
    def applePushID = column[String]("APPLE_PUSH_ID", O.Nullable)
    def * = sessionId ~ userId ~ created ~ lastAccessed ~ applePushID.? <> (PhantomSession.apply _, PhantomSession.unapply _)

    def owner = foreignKey("USER_FK", userId, UserTable)(_.id)
    //def userUnqiue = index("userUnique", userId, unique = true)
  }

}

