package com.sneeky.ds.user

import com.sneeky.ds.framework.Dates
import com.sneeky.ds.integration.amazon.S3Service
import org.joda.time.{ LocalDate }
import spray.http.MediaTypes._
import com.sneeky.model._
import com.sneeky.ds.framework.httpx._
import spray.json._
import com.sneeky.ds.{ BasicCrypto, DataHttpService }
import com.sneeky.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import java.util.UUID
import scala.concurrent.{ Await, Future }
import spray.http.StatusCodes

trait UserEndpoint extends DataHttpService with SneekyJsonProtocol with BasicCrypto {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  def s3Service : S3Service

  val userService = UserService(s3Service)
  val users = "users"

  /**
   * This should accept
   * @return
   */
  def register = pathPrefix(users / "register") {
    post {
      respondWithMediaType(`application/json`)
      complete(userService.register())
    }
  }

  def logout = pathPrefix(users / "logout") {
    authenticate(unverified _) { authenticationResult =>
      val (user, sessionId) = authenticationResult
      get {
        respondWithMediaType(`application/json`) {
          complete(userService.logout(sessionId.toString))
        }
      }
    }
  }

  //  def activeUser = pathPrefix(users / "active") {
  //    authenticate(unverified _) { authenticationResult =>
  //
  //      val (user, sessionId) = authenticationResult
  //
  //      get {
  //        respondWithMediaType(`application/json`) {
  //          log.trace(s"identify function invoked : $user")
  //
  //          import scala.concurrent.duration._
  //          val sessionObject = Await.result(userService.findFromSessionId(sessionId.toString), 1 seconds)
  //
  //          complete(
  //            Future.successful(
  //              userService.deriveExtraProperties(
  //                user,
  //                ActiveSneekyV2User(
  //                  user.birthday,
  //                  user.firstName.getOrElse(""),
  //                  user.lastName.getOrElse(""),
  //                  user.username,
  //                  user.profilePictureUrl.getOrElse(""),
  //                  user.newMessagePush
  //                ),
  //                sessionObject
  //              )
  //            )
  //          )
  //
  //        }
  //      }
  //    }
  //  }

  //  def update = pathPrefix(users / "update") {
  //    authenticate(unverified _) { authenticationResult =>
  //
  //      val (user, sessionId) = authenticationResult
  //
  //      parameter('sessionId) { session =>
  //        {
  //          respondWithMediaType(`application/json`) {
  //            entity(as[ShoutoutUserUpdateRequest]) { request =>
  //              complete(userService.updateUser(user.id.get, request))
  //            }
  //          }
  //        }
  //      }
  //    }
  //  }

  def updateSettings = pathPrefix(users / "settings") {
    authenticate(unverified _) { authenticationResult =>

      val (user, sessionId) = authenticationResult

      post {
        entity(as[SettingsRequest]) { pushRequest =>
          parameter('sessionId) { session =>
            complete {
              userService.updateSetting(
                user.id.get,
                pushRequest.settingType,
                pushRequest.settingValue
              )
            }
          }
        }
      }
    }
  }

  def updatePushNotifier = pathPrefix(users / "pushNotifier") {
    authenticate(unverified _) { authenticationResult =>

      val (user, sessionId) = authenticationResult

      post {
        entity(as[UpdatePushTokenRequest]) { pushTokenRequest =>
          parameter('sessionId) { session =>
            complete {
              userService.updatePushNotifier(
                UUID.fromString(session),
                pushTokenRequest.pushNotifierToken,
                pushTokenRequest.pushType
              )
            }
          }
        }
      }
    }
  }

  val userRoute =
    register ~
      logout ~
      //      update ~
      //      activeUser ~
      updateSettings ~
      updatePushNotifier

}
