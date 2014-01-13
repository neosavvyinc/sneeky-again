package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Success, Failure }
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
  def updateContacts(id : Long, contacts : String) : Future[StatusCode]
  def clearBlockList(id : Long) : Future[StatusCode]
}

object UserService {

  def apply()(implicit ec : ExecutionContext) = new UserService with DatabaseSupport with Logging {

    def register(registrationRequest : UserRegistration) : Future[PhantomUser] = {
      phantomUsers.register(registrationRequest)
    }

    def login(loginRequest : UserLogin) : Future[PhantomUser] = {
      phantomUsers.login(loginRequest)
    }

    def findById(id : Long) : Future[PhantomUser] = {
      phantomUsers.find(id)
    }

    def findContactsById(id : Long) : Future[List[PhantomUser]] = {
      phantomUsers.findContacts(id)
    }

    def updateContacts(id : Long, contactList : String) : Future[StatusCode] = {
      val session = db.createSession

      session.withTransaction {
        contacts.deleteAll(id)(session)
        //          .onComplete {
        //          //case Success(_) => Future.successful(phantomUsers.updateContacts(id, contactList))
        //          case Success(_) => {
        //            Future.failed(new Exception())
        //          }
        //          case Failure(ex) => {
        //            session.rollback()
        //            Future.failed(new Exception())
        //          }
        //        }
        session.rollback()
      }
      Future.successful(StatusCodes.OK)
    }

    def clearBlockList(id : Long) : Future[StatusCode] = {
      phantomUsers.clearBlockList(id)
    }
  }

}

