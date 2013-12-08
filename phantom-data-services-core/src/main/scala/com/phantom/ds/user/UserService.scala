package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future }
import com.phantom.model.{ RegistrationResponse, UserRegistration }
import com.phantom.ds.framework.exception.PhantomException
import scala.collection.mutable.{ Map => MMap }

trait UserService {

  def registerUser(registrationRequest : UserRegistration) : Future[RegistrationResponse]

}

class DuplicateUserException extends Exception with PhantomException {
  val code = 101
}

object UserService {

  def apply()(implicit ec : ExecutionContext) = MapbackedUserService /* new UserService {

    def registerUser(req : UserRegistration) : Future[StatusCode] = Future.successful(StatusCodes.OK)

  }*/

}

object MapbackedUserService extends UserService {

  val map : MMap[String, Int] = MMap.empty

  def registerUser(registrationRequest : UserRegistration) : Future[RegistrationResponse] = {

    map.get(registrationRequest.email) match {
      case Some(x) => Future.failed(new DuplicateUserException())
      case None => Future.successful {
        map(registrationRequest.email) = map.size + 1
        RegistrationResponse(200, registrationRequest.email)
      }
    }
  }

}