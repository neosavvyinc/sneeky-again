package com.phantom.ds.user

import spray.http.MediaTypes._
import com.phantom.model.UserRegistration
import com.phantom.ds.DataHttpService

trait UserEndpoint extends DataHttpService {

  val userService = UserService()

  val userRoute =
    pathPrefix("registration") {
      post {
        respondWithMediaType(`application/json`)
        entity(as[UserRegistration]) {
          reg =>
            complete {
              userService.registerUser(reg)
            }
        }
      }
    }

}
