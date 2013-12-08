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
}

object UserService {

  def apply()(implicit ec : ExecutionContext) = MapbackedUserService

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

    //    map.get(loginRequest.email) match {
    //      case Some(x) => Future.successful {
    //        UserResponse(200, "test")
    //      }
    //      case None => Future.failed(new Exception())
    //    }
    Future.successful {
      UserResponse(200, "test")
    }
  }

}
