package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future }
import spray.http.{ StatusCode, StatusCodes }
import spray.json._
import com.phantom.ds.framework.httpx._
import com.phantom.model._
import scala.collection.mutable.{ Map => MMap }
import com.phantom.ds.framework.Logging
import org.joda.time.LocalDate
import com.phantom.model.UserLogin
import com.phantom.model.PhantomUserTypes._
import com.phantom.model.PhantomUser
import com.phantom.model.UserRegistration
import com.phantom.dataAccess.DatabaseSupport

trait UserService {

  def register(registrationRequest : UserRegistration) : Future[PhantomUser]
  def login(loginRequest : UserLogin) : Future[PhantomUser]
  def findById(id : Long) : Future[PhantomUser]
  def findContactsById(id : Long) : Future[List[PhantomUser]]
  def updateContacts(id : Long, contacts : List[PhoneNumber]) : Future[StatusCode]
  def clearBlockList(id : Long) : Future[StatusCode]
}

object UserService extends DatabaseSupport {

  //def apply()(implicit ec : ExecutionContext) = MapbackedUserService
  def apply()(implicit ec : ExecutionContext) = new UserService {

    //def register(registrationRequest : UserRegistration) : Future[PhantomUser] = Unit
    def register(registrationRequest : UserRegistration) : Future[PhantomUser] = {
      users.register(registrationRequest)
    }

    def login(loginRequest : UserLogin) : Future[PhantomUser] = {
      users.login(loginRequest)
    }

    def findById(id : Long) : Future[PhantomUser] = {
      users.find(id)
    }

    def findContactsById(id : Long) : Future[List[PhantomUser]] = {
      //Future.successful(List(PhantomUser(None, "", new LocalDate("12345678"), true, "")))
      users.findContacts(id)
    }

    def updateContacts(id : Long, contacts : List[PhoneNumber]) : Future[StatusCode] = {
      users.updateContacts(id, contacts)
    }

    def clearBlockList(id : Long) : Future[StatusCode] = {
      users.clearBlockList(id)
    }
  }

}

