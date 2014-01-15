package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Success, Failure }
import spray.http.{ StatusCode, StatusCodes }
import spray.json._
import com.phantom.ds.framework.httpx._
import com.phantom.model._
import scala.collection.mutable.{ Map => MMap }
import com.phantom.ds.framework.Logging
import org.joda.time.{ DateTime, DateTimeZone, LocalDate }
import com.phantom.model.UserLogin
import com.phantom.model.PhantomUserTypes._
import com.phantom.model.PhantomUser
import com.phantom.model.UserRegistration
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID

trait UserService {

  def register(registrationRequest : UserRegistration) : Future[PhantomUser]
  def login(loginRequest : UserLogin) : Future[LoginSuccess]
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

    def login(loginRequest : UserLogin) : Future[LoginSuccess] = {
      for {
        user <- phantomUsers.login(loginRequest)
        existingSession <- sessions.existingSession(user.id.get)
        session <- getOrCreateSession(user, existingSession)
      } yield LoginSuccess(user, session.sessionId)
    }

    private def getOrCreateSession(user : PhantomUser, sessionOpt : Option[PhantomSession]) : Future[PhantomSession] = {
      val now = DateTime.now(DateTimeZone.UTC)
      sessionOpt.map(Future.successful).getOrElse(sessions.createSession(PhantomSession(UUID.randomUUID(), user.id.getOrElse(-1), now, now)))
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

