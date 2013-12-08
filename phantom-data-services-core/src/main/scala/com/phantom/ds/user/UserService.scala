package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future }
import com.phantom.model.{ RegistrationResponse, UserRegistration }
import com.phantom.ds.framework.exception.PhantomException
import scala.collection.mutable.{ Map => MMap }
import com.phantom.ds.framework.Logging

trait UserService {

  def registerUser(registrationRequest : UserRegistration) : Future[RegistrationResponse]

}

class DuplicateUserException extends Exception with PhantomException {
  val code = 101
  val message : String = "Email already in use"
}

object UserService {

  def apply()(implicit ec : ExecutionContext) = MapbackedUserService /* new UserService {

    def registerUser(req : UserRegistration) : Future[StatusCode] = Future.successful(StatusCodes.OK)

  }*/

}

object MapbackedUserService extends UserService with Logging {

  val map : MMap[String, Int] = MMap.empty

  def registerUser(registrationRequest : UserRegistration) : Future[RegistrationResponse] = {
    log.info(s"registering $registrationRequest")
    map.get(registrationRequest.email) match {
      case Some(x) => Future.failed(new DuplicateUserException())
      case None => Future.successful {
        map(registrationRequest.email) = map.size + 1
        RegistrationResponse(200, registrationRequest.email)
      }
    }
  }

}