package com.shoutout.ds.user

import com.netaporter.i18n.ResourceBundle
import com.shoutout.ds.integration.amazon.S3Service

import scala.concurrent.{ Await, ExecutionContext, Future, future }
import com.shoutout.model._
import com.shoutout.ds.framework.{ LocaleUtilities, Dates, Logging }
import com.shoutout.model.UserLogin
import com.shoutout.model.ShoutoutUser
import com.shoutout.dataAccess.DatabaseSupport
import java.util.{ Locale, UUID }
import com.shoutout.ds.framework.exception.ShoutoutException
import com.shoutout.ds.framework.email.{ MandrillConfiguration, MandrillUtil }
import com.shoutout.ds.BasicCrypto
import scala.slick.session.Session

trait UserService {

  def login(loginRequest : UserLogin) : Future[LoginSuccess]
  def facebookLogin(loginRequest : FacebookUserLogin) : Future[LoginSuccess]
  def register(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse]
  def logout(sessionId : String) : Future[Int]
  def updateUser(userId : Long, updateRequest : ShoutoutUserUpdateRequest) : Future[Int]
  def findFromSessionId(sessionId : String) : Future[ShoutoutSession]
  def updatePushNotifier(sessionUUID : UUID, applePushToken : String, mobilePushType : MobilePushType) : Future[Boolean]

}

object UserService extends BasicCrypto {

  def apply(s3Service : S3Service)(implicit ec : ExecutionContext) = new UserService with DatabaseSupport with Logging {

    val resourceBundle = ResourceBundle("messages/messages")

    def login(loginRequest : UserLogin) : Future[LoginSuccess] = {
      for {
        user <- shoutoutUsersDao.login(UserLogin(
          decryptField(loginRequest.email),
          decryptField(loginRequest.password)
        ))
        session <- sessionsDao.createSession(ShoutoutSession.newSession(
          user,
          deviceInfo = DeviceInfo(
            loginRequest.screenWidth,
            loginRequest.screenHeight,
            loginRequest.deviceModel,
            loginRequest.deviceLocale
          )))
      } yield LoginSuccess(session.sessionId)
    }

    def facebookLogin(loginRequest : FacebookUserLogin) : Future[LoginSuccess] = {

      def insertFriendIfNotExists(user : ShoutoutUser, targetFriendId : Long, deviceInfo : DeviceInfo) = {
        db.withSession { implicit session : Session =>
          val contactOption = contactsDao.findContactByIdForOwner(user, 1)
          contactOption match {
            case None => {
              contactsDao.insertFriendAssociation(user, ContactOrdering(None, Some(1), FriendType), 0)
              contactsDao.insertFriendAssociation(user, ContactOrdering(None, user.id, FriendType), 1)
            }
            case Some(c) => log.debug(s"User $user is already friends with the shoutout team")
          }

        }
      }

      var decryptedLoginRequest = FacebookUserLogin(
        decryptField(loginRequest.facebookId),
        loginRequest.firstName,
        loginRequest.lastName,
        loginRequest.birthdate
      )

      val deviceInfo = DeviceInfo(
        loginRequest.screenWidth,
        loginRequest.screenHeight,
        loginRequest.deviceModel,
        loginRequest.deviceLocale
      )

      for {
        userTuple <- shoutoutUsersDao.loginByFacebook(decryptedLoginRequest)
        session <- {
          if (userTuple._2 == false) {
            insertFriendIfNotExists(userTuple._1, 1, deviceInfo)
          }
          sessionsDao.createSession(ShoutoutSession.newSession(
            userTuple._1,
            deviceInfo = deviceInfo))
        }
      } yield LoginSuccess(session.sessionId)
    }

    def register(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse] = {
      val decryptedRequest = UserRegistrationRequest(
        decryptField(registrationRequest.email),
        decryptField(registrationRequest.password),
        screenWidth = registrationRequest.screenWidth,
        screenHeight = registrationRequest.screenHeight,
        deviceLocale = registrationRequest.deviceLocale,
        deviceModel = registrationRequest.deviceModel
      )

      for {
        _ <- Passwords.validate(decryptedRequest.password)
        registrationResponse <- doRegistration(decryptedRequest)
      } yield registrationResponse
    }

    def logout(sessionId : String) : Future[Int] = {
      sessionsDao.removeSession(UUID.fromString(sessionId))
    }

    def findFromSessionId(sessionId : String) : Future[ShoutoutSession] = {
      sessionsDao.sessionByUUID(UUID.fromString(sessionId))
    }

    def updateUser(userId : Long, updateRequest : ShoutoutUserUpdateRequest) : Future[Int] = {

      def validate() : Future[Boolean] = {
        future {

          updateRequest.username match {
            case Some(x) => {
              val isRestricted = shoutoutUsersDao.isUserNameRestricted(x)
              if (isRestricted)
                throw ShoutoutException.restrictedUsernameException
              else
                isRestricted
            }
            case _ => true
          }

        }
      }

      for {
        _ <- validate()
        persistentUser <- shoutoutUsersDao.findById(userId)
        rowsUpdated <- shoutoutUsersDao.update(persistentUser, updateRequest)
      } yield rowsUpdated

    }

    def updateSetting(userId : Long, pushSettingType : SettingType, value : Boolean) : Future[Boolean] = {
      future {
        shoutoutUsersDao.updateSetting(userId, pushSettingType, value)
      }
    }

    def deriveExtraProperties(user : ShoutoutUser, activeUser : ActiveShoutoutUser, shoutoutSession : ShoutoutSession) : ActiveShoutoutUser = {

      db.withSession { implicit session : Session =>
        activeUser.copy(
          profilePictureUrl = encryptField(activeUser.profilePictureUrl),
          receivedCount = shoutoutDao.countReceived(user),
          sentCount = shoutoutDao.countSent(user),
          sessionInvalid = shoutoutSession.sessionInvalid)
      }

    }

    def updatePushNotifier(sessionUUID : UUID, applePushToken : String, mobilePushType : MobilePushType) : Future[Boolean] = {
      future {

        val sessions = sessionsDao.findFromPushNotifierAndType(applePushToken, mobilePushType)
        sessions foreach (s => {
          sessionsDao.updatePushNotifier(s.sessionId, null, NullType)
        })

        sessionsDao.updatePushNotifier(sessionUUID, applePushToken, mobilePushType)
      }
    }

    private def doRegistration(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse] = {
      future {
        db.withTransaction { implicit s =>
          val user = shoutoutUsersDao.registerOperation(registrationRequest)

          val deviceInfo = DeviceInfo(
            registrationRequest.screenWidth,
            registrationRequest.screenHeight,
            registrationRequest.deviceModel,
            registrationRequest.deviceLocale
          )

          val session = sessionsDao.createSessionOperation(
            ShoutoutSession.newSession(user, deviceInfo = deviceInfo))

          //This one liner adds an association to the team shoutout account
          contactsDao.insertFriendAssociation(user, ContactOrdering(None, Some(1), FriendType), 0)
          contactsDao.insertFriendAssociation(user, ContactOrdering(None, user.id, FriendType), 1)

          RegistrationResponse(session.sessionId)
        }
      }
    }

  }

}

