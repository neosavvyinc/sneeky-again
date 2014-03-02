package com.phantom.ds.user

import spray.http.MediaTypes._
import com.phantom.model._
import com.phantom.ds.framework.httpx._
import spray.json._
import com.phantom.ds.{ BasicCrypto, DataHttpService }
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import java.util.UUID
import scala.concurrent.{ Await, Future }

trait UserEndpoint extends DataHttpService with PhantomJsonProtocol with BasicCrypto {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  val userService = UserService()

  val userRoute =
    pathPrefix("users" / "login") {
      authenticate(enter _) {
        bool =>
          post {
            respondWithMediaType(`application/json`)
            entity(as[UserLogin]) {
              reg =>
                complete(userService.login(UserLogin(
                  decryptField(reg.email),
                  decryptField(reg.password)
                )))
            }
          }
      }
    } ~
      pathPrefix("users" / "logout") {
        authenticate(unverified _) { user =>
          get { //todo:  authenticate should return case class of User/Session
            parameter('sessionId) { session =>
              respondWithMediaType(`application/json`) {
                complete(userService.logout(session))
              }
            }
          }
        }
      } ~
      pathPrefix("users" / "contacts") {
        authenticate(unverified _) { user =>
          post {
            respondWithMediaType(`application/json`) {
              entity(as[Map[String, List[String]]]) { phoneNumbers =>
                complete {
                  phoneNumbers.isDefinedAt("numbers") match {
                    case true  => userService.updateContacts(user.id.get, phoneNumbers("numbers"))
                    case false => "Invalid Dictionary Key"
                  }
                }
              }
            }
          }
        }
      } ~
      pathPrefix("users" / "clearblocklist") {
        authenticate(verified _) { user =>
          post {
            respondWithMediaType(`application/json`) {
              complete(userService.clearBlockList(user.id.get))
            }
          }
        }
      } ~
      pathPrefix("users" / "active") {
        authenticate(unverified _) { user =>
          parameter('sessionId) { session =>
            get {
              respondWithMediaType(`application/json`) {
                log.trace(s"identify function invoked : $user")

                //TODO: Hack time....need to clean this up
                //TODO: NS: This is fixed by modifiying the auth procedure to return both user and session
                //this is now the third time we've needed a handle on the session.
                //this is a quick fix in the auth code
                import scala.concurrent.duration._
                val sessionObject = Await.result(userService.findFromSessionId(session), 1 seconds)

                complete(
                  Future.successful(
                    SanitizedUser(
                      user.uuid,
                      encryptLocalDate(user.birthday),
                      user.status,
                      encryptOption(user.phoneNumber),
                      user.settingSound,
                      user.settingNewPicture,
                      user.mutualContactSetting,
                      sessionObject.sessionInvalid
                    )
                  )
                )

              }
            }
          }
        }
      } ~
      pathPrefix("users" / "pushNotifier") {
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
      } ~
      pathPrefix("users" / "settings") {
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
      } ~
      pathPrefix("users" / "forgotPassword") {
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
}
