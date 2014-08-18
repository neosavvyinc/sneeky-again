package com.shoutout.ds.user

import com.shoutout.ds.framework.Dates
import com.shoutout.ds.integration.amazon.S3Service
import org.joda.time.{ LocalDate }
import spray.http.MediaTypes._
import com.shoutout.model._
import com.shoutout.ds.framework.httpx._
import spray.json._
import com.shoutout.ds.{ BasicCrypto, DataHttpService }
import com.shoutout.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import java.util.UUID
import scala.concurrent.{ Await, Future }
import spray.http.StatusCodes

trait UserEndpoint extends DataHttpService with PhantomJsonProtocol with BasicCrypto {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  def s3Service : S3Service

  val userService = UserService(s3Service)
  val users = "users"

  /**
   * This should accept a email and password request
   *
   * @return - session or user id for the record
   */
  def loginEmail = pathPrefix(users / "login" / "email") {
    post {
      entity(as[UserLogin]) { loginRequest =>
        respondWithMediaType(`application/json`) {
          complete(userService.login(loginRequest))
        }
      }
    }
  }

  /**
   * This should log you in with Facebook if we have a record
   * for you, otherwise it should create a record
   * @return - session or user id for the record
   */
  def loginFacebook = pathPrefix(users / "login" / "facebook") {
    post {
      entity(as[FacebookUserLogin]) { loginRequest =>
        respondWithMediaType(`application/json`) {
          complete(userService.facebookLogin(loginRequest))
        }
      }
    }
  }

  /**
   * This should accept
   * @return
   */
  def registerEmail = pathPrefix(users / "register") {
    post {
      respondWithMediaType(`application/json`)
      entity(as[UserRegistrationRequest]) {
        reg =>
          log.trace(s"registering $reg")
          complete(userService.register(reg))
      }
    }
  }

  def logout = pathPrefix(users / "logout") {
    get { //todo:  authenticate should return case class of User/Session
      parameter('sessionId) { session =>
        respondWithMediaType(`application/json`) {
          complete(userService.logout(session))
        }
      }
    }
  }

  def activeUser = pathPrefix(users / "active") {
    authenticate(unverified _) { user =>
      parameter('sessionId) { session =>
        get {
          respondWithMediaType(`application/json`) {
            log.trace(s"identify function invoked : $user")

            import scala.concurrent.duration._
            val sessionObject = Await.result(userService.findFromSessionId(session), 1 seconds)

            complete(
              Future.successful(
                userService.deriveExtraProperties(
                  user,
                  ActiveShoutoutUser(
                    user.birthday,
                    user.firstName.getOrElse(""),
                    user.lastName.getOrElse(""),
                    user.username,
                    user.profilePictureUrl.getOrElse(""),
                    user.settingSound
                  ),
                  sessionObject
                )
              )
            )

          }
        }
      }
    }
  }

  def update = pathPrefix(users / "update") {
    authenticate(unverified _) { user =>
      parameter('sessionId) { session =>
        {
          respondWithMediaType(`application/json`) {
            entity(as[ShoutoutUserUpdateRequest]) { request =>
              complete(userService.updateUser(user.id.get, request))
            }
          }
        }
      }
    }
  }

  def updateProfilePhoto = pathPrefix(users / "update" / "photo") {
    val ByteJsonFormat = null
    authenticate(unverified _) { user =>
      post {
        formFields('image.as[Array[Byte]]) { (image) =>
          complete {
            userService.updateUserPhoto(image, user)
          }
        }
      }
    }
  }

  def updateSettings = pathPrefix(users / "settings") {
    authenticate(unverified _) { user =>
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
    authenticate(unverified _) { user =>
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

  def forgotPassword = pathPrefix(users / "forgotPassword") {
    post {
      entity(as[ForgotPasswordRequest]) { forgotPasswordRequest =>
        complete {
          userService.forgotPassword(
            forgotPasswordRequest.email
          )
        }
      }
    }
  }

  def changePassword = pathPrefix(users / "changePassword") {
    authenticate(unverified _) { user =>
      post {
        entity(as[ChangePasswordRequest]) { changePasswordRequest =>
          complete {
            userService.changePassword(user, changePasswordRequest)
          }
        }
      }
    }
  }

  val userRoute = loginFacebook ~
    loginEmail ~
    registerEmail ~
    logout ~
    update ~
    activeUser ~
    updateProfilePhoto ~
    updateSettings ~
    updatePushNotifier ~
    forgotPassword ~
    changePassword
}
