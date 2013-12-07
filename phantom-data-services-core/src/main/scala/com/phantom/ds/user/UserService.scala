package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future }
import spray.http.{ StatusCode, StatusCodes }
import com.phantom.model._
import com.phantom.ds.framework.exception.PhantomException
import scala.collection.mutable.{ Map => MMap }

trait UserService {

  def registerUser(registrationRequest : UserRegistration) : Future[UserResponse]
  def loginUser(loginRequest : UserLogin) : Future[UserResponse]

}

class DuplicateUserException extends Exception with PhantomException {
  val code = 101
  val message : String = "Email already in use"
}

object UserService {

  def apply()(implicit ec : ExecutionContext) = MapbackedUserService

  def registerUser(req : UserRegistration) : Future[StatusCode] = Future.successful(StatusCodes.OK)
  def loginUser(req : UserLogin) : Future[StatusCode] = Future.successful(StatusCodes.OK)
}

object MapbackedUserService extends UserService {

  val map : MMap[String, Int] = MMap.empty

  def registerUser(registrationRequest : UserRegistration) : Future[UserResponse] = {

    map.get(registrationRequest.email) match {
      case Some(x) => Future.failed(new DuplicateUserException())
      case None => Future.successful {
        map(registrationRequest.email) = map.size + 1
        UserResponse(200, registrationRequest.email)
      }
    }
  }

  def loginUser(loginRequest : UserLogin) : Future[UserResponse] = {
    Future.successful {
      UserResponse(200, "test")
    }
  }

}
