package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future }
import spray.http.{ StatusCode, StatusCodes }
import spray.json._
import com.phantom.ds.framework.httpx._
import com.phantom.model._
import com.phantom.ds.framework.exception.PhantomException
import scala.collection.mutable.{ Map => MMap }
import com.phantom.ds.framework.Logging

trait UserService {

  def registerUser(registrationRequest : UserRegistration) : Future[UserResponse]
  def loginUser(loginRequest : UserLogin) : Future[UserResponse]
  def findUser(id : Long) : Future[UserResponse]
}

class DuplicateUserException extends Exception with PhantomException {
  val code = 101
}

class NonexistantUserException extends Exception with PhantomException {
  val code = 103
}

object UserService {

  def apply()(implicit ec : ExecutionContext) = MapbackedUserService

}

object MapbackedUserService extends UserService with Logging with PhantomJsonProtocol {

  val map : MMap[String, String] = MMap.empty

  def registerUser(registrationRequest : UserRegistration) : Future[UserResponse] = {
    log.info(s"registering $registrationRequest")
    map.get(registrationRequest.email) match {
      case Some(x) => Future.failed(new DuplicateUserException())
      case None => Future.successful {
        map(registrationRequest.email) = registrationRequest.password
        UserResponse(200, registrationRequest.email)
      }
    }
  }

  def loginUser(loginRequest : UserLogin) : Future[UserResponse] = {
    log.info(s"logging in $loginRequest")
    map.get(loginRequest.email) match {
      case Some(password) =>
        if (password == loginRequest.password) Future.successful(UserResponse(200, "logged in!"))
        else Future.failed(new NonexistantUserException())
      case None => Future.failed(new NonexistantUserException())
    }
  }

  def findUser(id : Long) : Future[UserResponse] = {
    log.info(s"finding user with email => ???")
    map.get("test") match {
      case Some(x) => Future.successful {
        UserResponse(200, User(1, "test@test.com", "1/1/01", true).toJson.toString)
      }
      case None => Future.failed(new NonexistantUserException())
    }
  }

}
