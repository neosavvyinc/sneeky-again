package com.phantom.ds.user

import spray.http.MediaTypes._
import com.phantom.model._
import com.phantom.ds.framework.httpx._
import spray.json._
import com.phantom.ds.DataHttpService
import com.phantom.ds.framework.auth.Authenticator

trait UserEndpoint extends DataHttpService
    with PhantomJsonProtocol {
  this : Authenticator =>

  val userService = UserService()

  val userRoute =
    pathPrefix("users" / "register") {
      post {
        respondWithMediaType(`application/json`)
        entity(as[UserRegistration]) {
          reg =>
            complete {
              userService.registerUser(reg)
            }
        }
      }
    } ~
      pathPrefix("users" / "login") {
        post {
          respondWithMediaType(`application/json`)
          entity(as[UserLogin]) {
            reg =>
              complete(userService.loginUser(reg))
          }
        }
      } ~
      pathPrefix("users" / LongNumber / "contacts") { id =>
        authenticate(phantom _) { user =>
          get {
            respondWithMediaType(`application/json`) {
              complete(userService.findContactsForUser(id))
            }
          } ~
            post {
              respondWithMediaType(`application/json`) {
                entity(as[List[String]]) { contacts /* list of phone numbers */ =>
                  complete {
                    userService.updateContactsForUser(id, contacts)
                  }
                }
              }
            }
        }
      } ~
      pathPrefix("users" / LongNumber) { id =>
        authenticate(phantom _) { user =>
          get {
            respondWithMediaType(`application/json`) {
              complete(userService.findUser(id))
            }
          }
        }
      }
}
