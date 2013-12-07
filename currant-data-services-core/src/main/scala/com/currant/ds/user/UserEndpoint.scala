package com.currant.ds.user

import spray.http.MediaTypes._
import com.currant.model.{ CurrantUserJsonImplicits, CurrantUserRegistration }
import spray.json._
import com.currant.ds.DataHttpService

trait UserEndpoint extends DataHttpService {

  import CurrantUserJsonImplicits._

  val userService = UserService(db)

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