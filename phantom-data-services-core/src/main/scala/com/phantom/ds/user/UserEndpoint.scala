package com.phantom.ds.user

import spray.http.MediaTypes._
import com.phantom.model._
import com.phantom.ds.framework.httpx._
import spray.json._
import com.phantom.ds.DataHttpService
import com.phantom.dataAccess.{ DatabaseSupport }
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }

trait UserEndpoint extends DataHttpService with PhantomJsonProtocol {
  this : RequestAuthenticator with EntryPointAuthenticator with DatabaseSupport =>

  val userService = UserService()

  val userRoute =
    pathPrefix("users" / "register") {
      authenticate(enter _) {
        bool =>
          post {
            respondWithMediaType(`application/json`)
            entity(as[UserRegistration]) {
              reg =>
<<<<<<< HEAD
                complete(phantomUsers.registerUser(reg))
=======
                complete(userServiceDB.register(reg))
>>>>>>> 090318ba1c37d31acb790fc78ee15b9947d7e0d9
            }
          }
      }

    } ~
      pathPrefix("users" / "login") {
        authenticate(enter _) {
          bool =>
            post {
              respondWithMediaType(`application/json`)
              entity(as[UserLogin]) {
                reg =>
                  complete(phantomUsers.login(reg))
              }
            }
        }
      } ~
      pathPrefix("users" / LongNumber / "contacts") { id =>
        authenticate(request _) { user =>
          get {
            respondWithMediaType(`application/json`) {
<<<<<<< HEAD
              complete(phantomUsers.findContactsForUser(id))
=======
              complete(userServiceDB.findContactsById(id))
>>>>>>> 090318ba1c37d31acb790fc78ee15b9947d7e0d9
            }
          } ~
            post {
              respondWithMediaType(`application/json`) {
                entity(as[List[String]]) { contacts /* list of phone numbers */ =>
                  complete {
                    userServiceDB.updateContacts(id, contacts)
                  }
                }
              }
            }
        }
      } ~
      pathPrefix("users" / LongNumber / "clearblocklist") { id =>
        post {
          respondWithMediaType(`application/json`) {
            complete(userServiceDB.clearBlockList(id))
          }
        }
      } ~
      pathPrefix("users" / LongNumber) { id =>
        authenticate(request _) { user =>
          get {
            respondWithMediaType(`application/json`) {
<<<<<<< HEAD
              complete(phantomUsers.findUser(id))
=======
              complete(userServiceDB.findById(id))
>>>>>>> 090318ba1c37d31acb790fc78ee15b9947d7e0d9
            }
          }
        }
      }
}
