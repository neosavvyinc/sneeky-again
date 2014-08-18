package com.shoutout.model

import com.shoutout.dataAccess.Profile
import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import scala.slick.lifted.ColumnOption.DBType
import java.util.UUID
import com.shoutout.ds.framework.Dates

case class UserRegistrationRequest(email : String,
                                   password : String)

case class RegistrationResponse(sessionUUID : UUID)

case class RegistrationVerification(messageSid : String,
                                    accountSid : String,
                                    from : String,
                                    to : String,
                                    body : String,
                                    numMedia : Int)

case class UserLogin(email : String,
                     password : String)

case class Restriction(id : Option[Long], username : String)

case class ShoutoutUserUpdateRequest(birthday : Option[LocalDate],
                                     firstName : Option[String],
                                     lastName : Option[String],
                                     username : Option[String])

case class FacebookUserLogin(facebookId : String,
                             firstName : Option[String],
                             lastName : Option[String],
                             birthdate : Option[LocalDate])

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
  }

  def fromStringRep(str : String) : UserStatus = str.toLowerCase match {
    case "unverified" => Unverified
    case "verified"   => Verified
    case x            => throw new Exception(s"unrecognized user status $x")
  }
}

case object Unverified extends UserStatus

case object Verified extends UserStatus

sealed trait SettingType

object SettingType {

  def toStringRep(pushSettingType : SettingType) : String = pushSettingType match {
    case NewMessagePushNotifications => "newMessagePush"
  }

  def fromStringRep(str : String) : SettingType = str.toLowerCase match {
    case "newMessagePush" => NewMessagePushNotifications
    case x                => throw new Exception(s"unrecognized push setting $x")
  }

}

case object NewMessagePushNotifications extends SettingType

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

case class ShoutoutUser(id : Option[Long],
                        uuid : UUID,
                        facebookID : Option[String],
                        email : Option[String],
                        password : Option[String],
                        birthday : Option[LocalDate],
                        firstName : Option[String],
                        lastName : Option[String],
                        username : String,
                        profilePictureUrl : Option[String],
                        newMessagePush : Boolean = true)

case class ActiveShoutoutUser(
  birthday : Option[LocalDate],
  firstName : String,
  lastName : String,
  username : String,
  profilePictureUrl : String,
  newMessagePush : Boolean = true,
  sentCount : Int = 0,
  receivedCount : Int = 0,
  sessionInvalid : Boolean = false)

case class ForgotPasswordRequest(email : String)
case class ChangePasswordRequest(oldPassword : String, newPassword : String)

object ShoutoutSession {

  def newSession(user : ShoutoutUser, token : Option[String] = None) : ShoutoutSession = {
    val now = Dates.nowDT
    ShoutoutSession(UUID.randomUUID(), user.id.getOrElse(-1), now, now, token, None)
  }
}

case class ShoutoutSession(sessionId : UUID,
                           userId : Long,
                           created : DateTime,
                           lastAccessed : DateTime,
                           pushNotifierToken : Option[String] = None,
                           pushNotifierType : Option[MobilePushType] = None,
                           sessionInvalid : Boolean = false)

trait UserComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val UUIDMapper = MappedTypeMapper.base[UUID, String](UUIDConversions.toStringRep, UUIDConversions.fromStringRep)

  object UserTable extends Table[ShoutoutUser]("USERS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def uuid = column[UUID]("UUID", DBType("CHAR(36)"))
    def facebookID = column[String]("FACEBOOK_ID", DBType("VARCHAR(64)"), O.Nullable)
    def email = column[String]("EMAIL", DBType("VARCHAR(256)"), O.Nullable)
    def password = column[String]("PASSWORD", DBType("VARCHAR(300)"), O.Nullable)
    def birthday = column[LocalDate]("BIRTHDAY", O.Nullable)
    def firstName = column[String]("FIRST_NAME", O.Nullable)
    def lastName = column[String]("LAST_NAME", O.Nullable)
    def username = column[String]("USERNAME")
    def profilePictureUrl = column[String]("PROFILE_URL")
    def newMessagePush = column[Boolean]("PUSH_NOTIF")

    def * = id.? ~ uuid ~ facebookID.? ~ email.? ~ password.? ~ birthday.? ~ firstName.? ~ lastName.? ~ username ~ profilePictureUrl.? ~ newMessagePush <> (ShoutoutUser, ShoutoutUser.unapply _)
    def forInsert = * returning id

  }
}

trait UserSessionComponent { this : Profile with UserComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val MobilePushTypeMapper = MappedTypeMapper.base[MobilePushType, String](MobilePushType.toStringRep, MobilePushType.fromStringRep)

  object SessionTable extends Table[ShoutoutSession]("SESSIONS") {
    def sessionId = column[UUID]("SESSIONID")
    def userId = column[Long]("USERID")
    def created = column[DateTime]("CREATED")
    def lastAccessed = column[DateTime]("LASTACCESSED")
    def pushNotifierToken = column[String]("PUSH_NOTIFIER_TOKEN", O.Nullable)
    def pushNotifierType = column[MobilePushType]("PUSH_NOTIFIER_TYPE", O.Nullable)
    def sessionInvalidated = column[Boolean]("SESSION_INVALID")
    def * = sessionId ~ userId ~ created ~ lastAccessed ~ pushNotifierToken.? ~ pushNotifierType.? ~ sessionInvalidated <> (ShoutoutSession.apply _, ShoutoutSession.unapply _)

    def owner = foreignKey("USER_FK", userId, UserTable)(_.id)
    //def userUnqiue = index("userUnique", userId, unique = true)
  }

}

trait UserNameRestrictionsComponent { this : Profile =>

  import profile.simple._

  object RestrictionTable extends Table[Restriction]("USER_NAME_RESTRICTIONS") {

    def id = column[Long]("ID")
    def restrictedName = column[String]("RESTRICTED_NAME", O.Nullable)

    def * = id.? ~ restrictedName <> (Restriction.apply _, Restriction.unapply _)

  }

}

