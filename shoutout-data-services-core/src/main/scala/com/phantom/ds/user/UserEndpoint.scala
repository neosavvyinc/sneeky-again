package com.phantom.ds.user

import com.phantom.ds.framework.Dates
import com.phantom.ds.integration.amazon.S3Service
import org.joda.time.{ LocalDate }
import spray.http.MediaTypes._
import com.phantom.model._
import com.phantom.ds.framework.httpx._
import spray.json._
import com.phantom.ds.{ BasicCrypto, DataHttpService }
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
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
                ActiveShoutoutUser(
                  user.birthday,
                  user.firstName.getOrElse(""),
                  user.lastName.getOrElse(""),
                  user.username,
                  user.profilePictureUrl.getOrElse(""),
                  user.settingSound
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

  val userRoute = loginFacebook ~
    loginEmail ~
    registerEmail ~
    logout ~
    update ~
    activeUser ~
    updateProfilePhoto
}
