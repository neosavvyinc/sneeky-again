package com.sneeky.model

import com.sneeky.dataAccess.Profile
import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import scala.slick.lifted.ColumnOption.DBType
import java.util.UUID
import com.sneeky.ds.framework.Dates

case class DeviceInfo(screenWidth : Option[Int] = None,
                      screenHeight : Option[Int] = None,
                      deviceModel : Option[String] = None,
                      deviceLocale : Option[String] = None)

case class ShoutoutUserUpdateRequest(birthday : Option[LocalDate],
                                     firstName : Option[String],
                                     lastName : Option[String],
                                     username : Option[String])

case class UpdatePushTokenRequest(pushNotifierToken : String,
                                  pushType : MobilePushType)

case class SettingsRequest(settingValue : Boolean,
                           settingType : SettingType)

sealed trait UserStatus

object UserStatus {
  def toStringRep(status : UserStatus) : String = status match {
    case Unverified => "unverified"
    case Verified   => "verified"
    case Admin      => "admin"
  }

  def fromStringRep(str : String) : UserStatus = str.toLowerCase match {
    case "unverified" => Unverified
    case "verified"   => Verified
    case "admin"      => Admin
    case x            => throw new Exception(s"unrecognized user status $x")
  }
}

case object Unverified extends UserStatus
case object Verified extends UserStatus
case object Admin extends UserStatus

sealed trait SettingType

object SettingType {

  def toStringRep(pushSettingType : SettingType) : String = pushSettingType match {
    case NewMessagePushNotifications => "newMessagePush"
  }

  def fromStringRep(str : String) : SettingType = str match {
    case "newMessagePush" => NewMessagePushNotifications
    case x                => throw new Exception(s"unrecognized push setting $x")
  }

}

case object NewMessagePushNotifications extends SettingType

sealed trait MobilePushType

object MobilePushType {

  def toStringRep(pushType : MobilePushType) : String = pushType match {
    case NullType => null
    case Apple    => "apple"
    case Android  => "android"
  }

  def fromStringRep(str : String) : MobilePushType = str match {
    case null      => NullType
    case "apple"   => Apple
    case "android" => Android
    case x         => throw new Exception(s"unrecognized push type $x")
  }

}

case object NullType extends MobilePushType

case object Apple extends MobilePushType

case object Android extends MobilePushType

object UUIDConversions {

  val toStringRep : (UUID => String) = _.toString

  val fromStringRep = UUID.fromString _

}

case class SneekyV2User(id : Option[Long],
                        uuid : UUID,
                        facebookID : Option[String],
                        email : Option[String],
                        password : Option[String],
                        birthday : Option[LocalDate],
                        firstName : Option[String],
                        lastName : Option[String],
                        username : String,
                        profilePictureUrl : Option[String],
                        newMessagePush : Boolean = true,
                        userStatus : UserStatus = Unverified,
                        lastAccessed : DateTime = Dates.nowDT)

case class ActiveSneekyV2User(
  birthday : Option[LocalDate],
  firstName : String,
  lastName : String,
  username : String,
  profilePictureUrl : String,
  newMessagePush : Boolean = true,
  sentCount : Int = 0,
  receivedCount : Int = 0,
  sessionInvalid : Boolean = false)

object SneekySession {

  def newSession(user : SneekyV2User, token : Option[String] = None, deviceInfo : DeviceInfo) : SneekySession = {
    val now = Dates.nowDT
    SneekySession(UUID.randomUUID(), user.id.getOrElse(-1), now, now, token, None,
      screenWidth = deviceInfo.screenWidth,
      screenHeight = deviceInfo.screenHeight,
      deviceModel = deviceInfo.deviceModel,
      deviceLocale = deviceInfo.deviceLocale
    )
  }
}

case class SneekySession(sessionId : UUID,
                         userId : Long,
                         created : DateTime,
                         lastAccessed : DateTime,
                         pushNotifierToken : Option[String] = None,
                         pushNotifierType : Option[MobilePushType] = None,
                         sessionInvalid : Boolean = false,
                         screenWidth : Option[Int] = None,
                         screenHeight : Option[Int] = None,
                         deviceModel : Option[String] = None,
                         deviceLocale : Option[String] = None)

trait UserComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val UUIDMapper = MappedTypeMapper.base[UUID, String](UUIDConversions.toStringRep, UUIDConversions.fromStringRep)
  implicit val UserStatusMapper = MappedTypeMapper.base[UserStatus, String](UserStatus.toStringRep, UserStatus.fromStringRep)

  object UserTable extends Table[SneekyV2User]("USERS") {
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
    def userStatus = column[UserStatus]("USER_STATUS")
    def lastAccessed = column[DateTime]("LASTACCESSED", O.Nullable)

    def * = id.? ~
      uuid ~
      facebookID.? ~
      email.? ~
      password.? ~
      birthday.? ~
      firstName.? ~
      lastName.? ~
      username ~
      profilePictureUrl.? ~
      newMessagePush ~
      userStatus ~
      lastAccessed <> (SneekyV2User, SneekyV2User.unapply _)
    def forInsert = * returning id

  }
}

trait UserSessionComponent { this : Profile with UserComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val MobilePushTypeMapper = MappedTypeMapper.base[MobilePushType, String](MobilePushType.toStringRep, MobilePushType.fromStringRep)

  object SessionTable extends Table[SneekySession]("SESSIONS") {
    def sessionId = column[UUID]("SESSIONID")
    def userId = column[Long]("USERID")
    def created = column[DateTime]("CREATED")
    def lastAccessed = column[DateTime]("LASTACCESSED")
    def pushNotifierToken = column[String]("PUSH_NOTIFIER_TOKEN", O.Nullable)
    def pushNotifierType = column[MobilePushType]("PUSH_NOTIFIER_TYPE", O.Nullable)
    def sessionInvalidated = column[Boolean]("SESSION_INVALID")
    def screenWidth = column[Int]("SCREEN_WIDTH", O.Nullable)
    def screenHeight = column[Int]("SCREEN_HEIGHT", O.Nullable)
    def deviceModel = column[String]("DEVICE_MODEL", O.Nullable)
    def locale = column[String]("LOCALE", O.Nullable)

    def * = sessionId ~
      userId ~
      created ~
      lastAccessed ~
      pushNotifierToken.? ~
      pushNotifierType.? ~
      sessionInvalidated ~
      screenWidth.? ~
      screenHeight.? ~
      deviceModel.? ~
      locale.? <> (SneekySession.apply _, SneekySession.unapply _)

    def owner = foreignKey("USER_FK", userId, UserTable)(_.id)
    //def userUnqiue = index("userUnique", userId, unique = true)
  }

}