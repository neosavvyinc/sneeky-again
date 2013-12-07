package com.phantom.ds.user

import spray.http.MediaTypes._
import com.phantom.model.{ CurrantUserJsonImplicits, CurrantUserRegistration }
import spray.json._
import com.phantom.ds.DataHttpService

trait UserEndpoint extends DataHttpService {

  import CurrantUserJsonImplicits._

  val userService = UserService()

  val userRoute =
    pathPrefix("registration") {
      post {
        respondWithMediaType(`application/json`)
        entity(as[CurrantUserRegistration]) {
          reg =>
            complete {
              userService.registerUser(reg)
            }
        }
      }
    }

}