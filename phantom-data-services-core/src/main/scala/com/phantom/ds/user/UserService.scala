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
  def findContactsForUser(id : Long) : Future[UserResponse]
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

  val map : MMap[Int, UserLogin] = MMap.empty
  var contactList = List[String]();

  def registerUser(registrationRequest : UserRegistration) : Future[UserResponse] = {
    log.info(s"registering $registrationRequest")
    map.values.collectFirst {
      case u : UserLogin if u.email == registrationRequest.email => Future.failed(new DuplicateUserException())
    }.getOrElse {
      map(map.size + 1) = UserLogin(registrationRequest.email, registrationRequest.password)
      Future.successful(UserResponse(200, registrationRequest.email))
    }

  }

  def loginUser(loginRequest : UserLogin) : Future[UserResponse] = {
    log.info(s"logging in $loginRequest")
    map.values.collectFirst {
      case u : UserLogin if u.email == loginRequest.email && u.password == loginRequest.password =>
        Future.successful(UserResponse(200, "logged in!"))
    }.getOrElse(Future.failed(new NonexistantUserException()))
  }

  def findUser(id : Long) : Future[UserResponse] = {
    log.info(s"finding contacts for user with id => $id")
    map.get(id.toInt)
      .map(u => Future.successful(UserResponse(200, User(id, u.email, "1/1/01", true).toJson.toString)))
      .getOrElse(Future.failed(new NonexistantUserException()))
  }

  def findContactsForUser(id : Long) : Future[UserResponse] = {
    log.info(s"finding contacts for user with id => $id")
    map.get(id.toInt)
      .map(u => Future.successful(UserResponse(200, contactList.toJson.toString)))
      .getOrElse(Future.failed(new NonexistantUserException()))
  }

  def updateContactsForUser(id : Long, contacts : List[String]) : Future[UserResponse] = {
    log.info(s"updating contacts for use with id => $id")
    map.get(id.toInt)
      .map(u => Future.successful {
        contactList = contacts
        UserResponse(200, contactList.toJson.toString)
      })
      .getOrElse(Future.failed(new NonexistantUserException()))
  }
}
