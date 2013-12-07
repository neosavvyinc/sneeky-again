package com.phantom.ds.user

import com.phantom.model._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.future
import com.phantom.model.CurrantUserRegistration
import spray.http.{ StatusCodes, StatusCode }

trait UserService {

  def registerUser(registrationRequest : CurrantUserRegistration) : Future[StatusCode]

}

class DuplicateUserException(message : String = "Email already in use") extends Exception(message) {
  val code = 101
}

object UserService {

  def apply()(implicit ec : ExecutionContext) = new UserService {

    def registerUser(req : CurrantUserRegistration) : Future[StatusCode] = {
      future {
        StatusCodes.OK
      }
    }

  }

}