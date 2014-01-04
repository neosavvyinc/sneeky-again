package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future }
import spray.http.{ StatusCode, StatusCodes }
import spray.json._
import com.phantom.ds.framework.httpx._
import com.phantom.model._
import com.phantom.ds.framework.exception.PhantomException
import scala.collection.mutable.{ Map => MMap }
import com.phantom.ds.framework.Logging
import org.joda.time.LocalDate
import com.phantom.model.UserLogin
import com.phantom.model.PhantomUser
import com.phantom.model.UserRegistration
import com.phantom.model.ClientSafeUserResponse
import com.phantom.dataAccess.DBConfig

trait UserService {

  def registerUser(registrationRequest : UserRegistration) : Future[ClientSafeUserResponse]
  def loginUser(loginRequest : UserLogin) : Future[ClientSafeUserResponse]
  def findUser(id : Long) : Future[ClientSafeUserResponse]
  def findContactsForUser(id : Long) : Future[List[PhantomUserDeleteMe]]
  def updateContactsForUser(id : Long, contacts : List[String]) : Future[List[PhantomUserDeleteMe]]
  def clearBlockList(id : Long) : Future[String]
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
  var contactList = List[String]()

  def registerUser(registrationRequest : UserRegistration) : Future[ClientSafeUserResponse] = {
    log.info(s"registering $registrationRequest")
    map.values.collectFirst {
      case u : UserLogin if u.email == registrationRequest.email => Future.failed(new DuplicateUserException())
    }.getOrElse {
      map(map.size + 1) = UserLogin(registrationRequest.email, registrationRequest.password)
      log.info("added user to map")
      Future.successful(ClientSafeUserResponse(registrationRequest.email, "", registrationRequest.birthday, true, false))
    }

  }

  def loginUser(loginRequest : UserLogin) : Future[ClientSafeUserResponse] = {
    log.info(s"logging in $loginRequest")
    map.values.collectFirst {
      case u : UserLogin if u.email == loginRequest.email && u.password == loginRequest.password =>
        Future.successful(ClientSafeUserResponse(u.email, "", "2001-01-01", true, false))
    }.getOrElse(Future.failed(new NonexistantUserException()))
  }

  def findUser(id : Long) : Future[ClientSafeUserResponse] = {
    log.info(s"finding contacts for user with id => $id")
    map.get(id.toInt)
      .map(u => Future.successful(ClientSafeUserResponse(u.email, "", "2001-01-01", true, false)))
      .getOrElse(Future.failed(new NonexistantUserException()))
  }

  def findContactsForUser(id : Long) : Future[List[PhantomUserDeleteMe]] = {
    log.info(s"finding contacts for user with id => $id")
    map.get(id.toInt)
      .map(u => Future.successful(contactList.map(PhantomUserDeleteMe(_))))
      .getOrElse(Future.failed(new NonexistantUserException()))
  }

  def updateContactsForUser(id : Long, contacts : List[String]) : Future[List[PhantomUserDeleteMe]] = {
    log.info(s"updating contacts for user with id => $id")
    map.get(id.toInt)
      .map { u =>
        contactList = contacts
        Future.successful(contactList.map(PhantomUserDeleteMe(_)))
      }
      .getOrElse(Future.failed(new NonexistantUserException()))
  }

  def clearBlockList(id : Long) : Future[String] = {
    log.info(s"clearing block list for use with id => $id")
    map.get(id.toInt)
      .map(u => Future.successful {
        contactList = List[String]()
        "OK"
      })
      .getOrElse(Future.failed(new NonexistantUserException()))
  }
}
