package com.shoutout.ds.user

import java.util.UUID

import com.netaporter.i18n.ResourceBundle
import com.shoutout.dataAccess.DatabaseSupport
import com.shoutout.ds.BasicCrypto
import com.shoutout.ds.framework.Logging
import com.shoutout.ds.framework.exception.ShoutoutException
import com.shoutout.ds.integration.amazon.S3Service
import com.shoutout.model.{ ShoutoutUser, _ }

import scala.concurrent.{ ExecutionContext, Future, future }
import scala.slick.session.Session

trait UserService {

  def logout(sessionId : String) : Future[Int]
  def updateUser(userId : Long, updateRequest : ShoutoutUserUpdateRequest) : Future[Int]
  def findFromSessionId(sessionId : String) : Future[ShoutoutSession]
  def updatePushNotifier(sessionUUID : UUID, applePushToken : String, mobilePushType : MobilePushType) : Future[Boolean]

}

object UserService extends BasicCrypto {

  def apply(s3Service : S3Service)(implicit ec : ExecutionContext) = new UserService with DatabaseSupport with Logging {

    val resourceBundle = ResourceBundle("messages/messages")

    def register() : Future[String] = {
      future {
        UUID.randomUUID().toString
      }
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

  }

}

