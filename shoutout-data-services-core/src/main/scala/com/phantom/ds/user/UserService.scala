package com.phantom.ds.user

import com.phantom.ds.integration.amazon.S3Service

import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.model._
import com.phantom.ds.framework.Logging
import com.phantom.model.UserLogin
import com.phantom.model.ShoutoutUser
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID
import com.phantom.ds.framework.exception.ShoutoutException
import com.phantom.ds.framework.email.{ MandrillConfiguration, MandrillUtil }
import com.phantom.ds.BasicCrypto
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

    def login(loginRequest : UserLogin) : Future[LoginSuccess] = {
      for {
        user <- shoutoutUsersDao.login(loginRequest)
        session <- sessionsDao.createSession(ShoutoutSession.newSession(user))
      } yield LoginSuccess(session.sessionId)
    }

    def facebookLogin(loginRequest : FacebookUserLogin) : Future[LoginSuccess] = {
      for {
        user <- shoutoutUsersDao.loginByFacebook(loginRequest)
        session <- sessionsDao.createSession(ShoutoutSession.newSession(user))
      } yield LoginSuccess(session.sessionId)
    }

    def register(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse] = {
      for {
        _ <- Passwords.validate(registrationRequest.password)
        registrationResponse <- doRegistration(registrationRequest)
      } yield registrationResponse
    }

    def logout(sessionId : String) : Future[Int] = {
      sessionsDao.removeSession(UUID.fromString(sessionId))
    }

    def findFromSessionId(sessionId : String) : Future[ShoutoutSession] = {
      sessionsDao.sessionByUUID(UUID.fromString(sessionId))
    }

    def updateUser(userId : Long, updateRequest : ShoutoutUserUpdateRequest) : Future[Int] = {

      for {
        persistentUser <- shoutoutUsersDao.findById(userId)
        rowsUpdated <- shoutoutUsersDao.update(persistentUser, updateRequest)
      } yield rowsUpdated

    }

    def updateUserPhoto(image : Array[Byte], user : ShoutoutUser) : Future[String] = {

      def saveUserPhotoForProfile(image : Array[Byte]) : String = {
        s3Service.saveProfileImage(image)
      }

      future {
        val url = saveUserPhotoForProfile(image)
        shoutoutUsersDao.updateProfilePicUrl(user.copy(profilePictureUrl = Some(url)))
        url
      }

    }

    def updateSetting(userId : Long, pushSettingType : SettingType, value : Boolean) : Future[Boolean] = {
      future {
        shoutoutUsersDao.updateSetting(userId, pushSettingType, value)
      }
    }

    def deriveExtraProperties(user : ShoutoutUser, activeUser : ActiveShoutoutUser) : ActiveShoutoutUser = {

      db.withSession { implicit session : Session =>
        activeUser.copy(
          receivedCount = shoutoutDao.countReceived(user),
          sentCount = shoutoutDao.countSent(user))
      }

    }

    def updatePushNotifier(sessionUUID : UUID, applePushToken : String, mobilePushType : MobilePushType) : Future[Boolean] = {
      future {
        sessionsDao.updatePushNotifier(sessionUUID, applePushToken, mobilePushType)
      }
    }

    private def doRegistration(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse] = {
      future {
        db.withTransaction { implicit s =>
          val user = shoutoutUsersDao.registerOperation(registrationRequest)
          val session = sessionsDao.createSessionOperation(ShoutoutSession.newSession(user))
          RegistrationResponse(session.sessionId)
        }
      }
    }

  }

}

