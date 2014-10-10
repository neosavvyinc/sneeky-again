package com.sneeky.ds.user

import java.util.UUID

import com.netaporter.i18n.ResourceBundle
import com.sneeky.dataAccess.DatabaseSupport
import com.sneeky.ds.BasicCrypto
import com.sneeky.ds.framework.{ Dates, Logging }
import com.sneeky.ds.framework.exception.ShoutoutException
import com.sneeky.ds.integration.amazon.S3Service
import com.sneeky.model.{ SneekyV2User, _ }

import scala.concurrent.{ ExecutionContext, Future, future }
import scala.slick.session.Session

trait UserService {

  def logout(sessionId : String) : Future[Int]
  //  def updateUser(userId : Long, updateRequest : ShoutoutUserUpdateRequest) : Future[Int]
  def findFromSessionId(sessionId : String) : Future[SneekySession]
  def updatePushNotifier(sessionUUID : UUID, applePushToken : String, mobilePushType : MobilePushType) : Future[Boolean]

}

object UserService extends BasicCrypto {

  def apply(s3Service : S3Service)(implicit ec : ExecutionContext) = new UserService with DatabaseSupport with Logging {

    val resourceBundle = ResourceBundle("messages/messages")

    def register() : Future[UUID] = {
      future {

        db.withTransaction { implicit session : Session =>
          val userId = UUID.randomUUID()
          val user = sneekyUserDao.insertUser(SneekyV2User(None, userId, true, Dates.nowDT))
          val sessionId = sessionsDao.createSessionOperation(SneekySession.newSession(user))

          sessionId.sessionId
        }

      }
    }

    def logout(sessionId : String) : Future[Int] = {
      sessionsDao.removeSession(UUID.fromString(sessionId))
    }

    def findFromSessionId(sessionId : String) : Future[SneekySession] = {
      sessionsDao.sessionByUUID(UUID.fromString(sessionId))
    }

    //    def updateUser(userId : Long, updateRequest : ShoutoutUserUpdateRequest) : Future[Int] = {
    //
    //      def validate() : Future[Boolean] = {
    //        future {
    //
    //          updateRequest.username match {
    //            case Some(x) => {
    //              val isRestricted = shoutoutUsersDao.isUserNameRestricted(x)
    //              if (isRestricted)
    //                throw ShoutoutException.restrictedUsernameException
    //              else
    //                isRestricted
    //            }
    //            case _ => true
    //          }
    //
    //        }
    //      }
    //
    //      for {
    //        _ <- validate()
    //        persistentUser <- shoutoutUsersDao.findById(userId)
    //        rowsUpdated <- shoutoutUsersDao.update(persistentUser, updateRequest)
    //      } yield rowsUpdated
    //
    //    }

    def updateSetting(userId : Long, pushSettingType : SettingType, value : Boolean) : Future[Boolean] = {
      future {
        sneekyUserDao.updateSetting(userId, pushSettingType, value)
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

