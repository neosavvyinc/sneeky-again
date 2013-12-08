package com.phantom.ds.user

import spray.http.MediaTypes._
import spray.http.StatusCodes
import com.phantom.model._
import com.phantom.model.UserJsonImplicits._
import spray.json._
import com.phantom.ds.DataHttpService

trait UserEndpoint extends DataHttpService {

  val userService = UserService()

  val userRoute =
    pathPrefix("user" / "register") {
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
      pathPrefix("user" / "login") {
        post {
          respondWithMediaType(`application/json`)
          entity(as[UserLogin]) {
            reg =>
              complete(userService.loginUser(reg))
          }
        }
      } ~
      pathPrefix("user" / LongNumber) { id =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              StatusCodes.OK
            }
          }
        }
      } ~
      pathPrefix("user" / LongNumber / "contacts") { id =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              StatusCodes.OK
            }
          }
        } ~
          post {
            respondWithMediaType(`application/json`) {
              entity(as[List[String]]) { contacts /* list of phone numbers */ =>
                complete {
                  StatusCodes.OK
                }
              }
            }
          }
      }

}
