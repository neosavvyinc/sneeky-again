package com.phantom.ds.user

import spray.http.MediaTypes._
import com.phantom.model._
import com.phantom.ds.framework.httpx._
import spray.json._
import com.phantom.ds.DataHttpService
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import java.util.UUID
import scala.concurrent.Future

trait UserEndpoint extends DataHttpService with PhantomJsonProtocol {
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
                complete(userService.login(reg))
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
        authenticate(verified _) { user =>
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
      pathPrefix("users") {
        authenticate(unverified _) { user =>
          get {
            respondWithMediaType(`application/json`) {
              log.trace(s"identify function invoked : $user")
              complete(Future.successful(SanitizedUser(user.uuid, user.birthday, user.status)))
            }
          }
        }
      } ~
      pathPrefix("users" / "pushNotifier") {
        authenticate(verified _) { user =>
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
      pathPrefix("users" / "pushSettings") {
        authenticate(verified _) { user =>
          post {
            entity(as[PushSettingsRequest]) { pushRequest =>
              parameter('sessionId) { session =>
                complete {
                  userService.updatePushSetting(
                    user.id.get,
                    pushRequest.pushSettingType,
                    pushRequest.settingValue
                  )
                }
              }
            }
          }
        }
      }
}
