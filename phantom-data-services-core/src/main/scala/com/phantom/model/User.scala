package com.phantom.model

import com.phantom.dataAccess.Profile
import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import scala.slick.lifted.ColumnOption.DBType
import java.util.UUID
import com.phantom.ds.framework.Dates

case class UserRegistrationRequest(email : String,
                                   birthday : String,
                                   password : String)

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

case class UpdatePushTokenRequest(pushNotifierToken : String,
                                  pushType : MobilePushType)

case class SettingsRequest(settingValue : Boolean,
                           settingType : SettingType)

sealed trait UserStatus

object UserStatus {
  def toStringRep(status : UserStatus) : String = status match {
    case Unverified => "unverified"
    case Verified   => "verified"
    case Stub       => "stub"
  }

  def fromStringRep(str : String) : UserStatus = str.toLowerCase match {
    case "unverified" => Unverified
    case "verified"   => Verified
    case "stub"       => Stub
    case x            => throw new Exception(s"unrecognized user status $x")
  }
}

case object Unverified extends UserStatus

case object Verified extends UserStatus

case object Stub extends UserStatus

sealed trait SettingType

object SettingType {

  def toStringRep(pushSettingType : SettingType) : String = pushSettingType match {
    case NotificationOnNewPicture => "picture-received"
    case SoundOnNewNotification   => "sound-on-new-item"
    case MutualContactMessaging   => "mutual-contacts"
  }

  def fromStringRep(str : String) : SettingType = str.toLowerCase match {
    case "picture-received"  => NotificationOnNewPicture
    case "sound-on-new-item" => SoundOnNewNotification
    case "mutual-contacts"   => MutualContactMessaging
    case x                   => throw new Exception(s"unrecognized push setting $x")
  }

}

case object SoundOnNewNotification extends SettingType

case object NotificationOnNewPicture extends SettingType

case object MutualContactMessaging extends SettingType

sealed trait MobilePushType

object MobilePushType {

  def toStringRep(pushType : MobilePushType) : String = pushType match {
    case Apple   => "apple"
    case Android => "android"
  }

  def fromStringRep(str : String) : MobilePushType = str.toLowerCase match {
    case "apple"   => Apple
    case "android" => Android
    case x         => throw new Exception(s"unrecognized push type $x")
  }

}

case object Apple extends MobilePushType

case object Android extends MobilePushType

object UUIDConversions {

  val toStringRep : (UUID => String) = _.toString

  val fromStringRep = UUID.fromString _

}

case class PhantomUser(id : Option[Long],
                       uuid : UUID,
                       email : Option[String],
                       password : Option[String],
                       birthday : Option[LocalDate],
                       active : Boolean,
                       phoneNumber : Option[String],
                       status : UserStatus = Unverified,
                       invitationCount : Int = 1,
                       settingSound : Boolean = true,
                       settingNewPicture : Boolean = true,
                       mutualContactSetting : Boolean = false)

case class SanitizedUser(uuid : UUID,
                         birthday : Option[String],
                         status : UserStatus,
                         phoneNumber : Option[String],
                         settingSound : Boolean,
                         settingNewPicture : Boolean,
                         mutualContactSetting : Boolean,
                         sessionInvalid : Boolean = false)

case class SanitizedContact(birthday : Option[String],
                            status : UserStatus,
                            phoneNumber : Option[String])

case class ForgotPasswordRequest(email : String)

object PhantomSession {

  def newSession(user : PhantomUser, token : Option[String] = None) : PhantomSession = {
    val now = Dates.nowDT
    PhantomSession(UUID.randomUUID(), user.id.getOrElse(-1), now, now, token, None)
  }
}

case class PhantomSession(sessionId : UUID,
                          userId : Long,
                          created : DateTime,
                          lastAccessed : DateTime,
                          pushNotifierToken : Option[String] = None,
                          pushNotifierType : Option[MobilePushType] = None,
                          sessionInvalid : Boolean = false)

trait UserComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val UserStatusMapper = MappedTypeMapper.base[UserStatus, String](UserStatus.toStringRep, UserStatus.fromStringRep)

  implicit val UUIDMapper = MappedTypeMapper.base[UUID, String](UUIDConversions.toStringRep, UUIDConversions.fromStringRep)

  object UserTable extends Table[PhantomUser]("USERS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def uuid = column[UUID]("UUID")
    def email = column[String]("EMAIL", DBType("VARCHAR(256)"), O.Nullable)
    def password = column[String]("PASSWORD", DBType("VARCHAR(300)"), O.Nullable)
    def birthday = column[LocalDate]("BIRTHDAY", O.Nullable)
    def active = column[Boolean]("ACTIVE")
    def phoneNumber = column[String]("PHONE_NUMBER", O.Nullable)
    def status = column[UserStatus]("STATUS")
    def settingSound = column[Boolean]("SOUND_NOTIF")
    def settingNewPicture = column[Boolean]("NEW_PICTURE_NOTIF")
    def mutualContactSetting = column[Boolean]("MUTUAL_CONTACT_ONLY")
    def invitationCount = column[Int]("INVITATION_COUNT")

    def * = id.? ~ uuid ~ email.? ~ password.? ~ birthday.? ~ active ~ phoneNumber.? ~ status ~ invitationCount ~ settingSound ~ settingNewPicture ~ mutualContactSetting <> (PhantomUser, PhantomUser.unapply _)
    def forInsert = * returning id

  }
}

trait UserSessionComponent { this : Profile with UserComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val MobilePushTypeMapper = MappedTypeMapper.base[MobilePushType, String](MobilePushType.toStringRep, MobilePushType.fromStringRep)

  object SessionTable extends Table[PhantomSession]("SESSIONS") {
    def sessionId = column[UUID]("SESSIONID")
    def userId = column[Long]("USERID")
    def created = column[DateTime]("CREATED")
    def lastAccessed = column[DateTime]("LASTACCESSED")
    def pushNotifierToken = column[String]("PUSH_NOTIFIER_TOKEN", O.Nullable)
    def pushNotifierType = column[MobilePushType]("PUSH_NOTIFIER_TYPE", O.Nullable)
    def sessionInvalidated = column[Boolean]("SESSION_INVALID")
    def * = sessionId ~ userId ~ created ~ lastAccessed ~ pushNotifierToken.? ~ pushNotifierType.? ~ sessionInvalidated <> (PhantomSession.apply _, PhantomSession.unapply _)

    def owner = foreignKey("USER_FK", userId, UserTable)(_.id)
    //def userUnqiue = index("userUnique", userId, unique = true)
  }

}

