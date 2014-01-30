package com.phantom.ds.user

import scala.concurrent.{ Promise, ExecutionContext, Future }
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
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID

trait UserService {

  def login(loginRequest : UserLogin) : Future[LoginSuccess]
  def logout(sessionId : String) : Future[Unit]
  def findById(id : Long) : Future[PhantomUser]
  def findContactsById(id : Long) : Future[List[PhantomUser]]
  def updateContacts(id : Long, contacts : List[String]) : Future[List[Long]]
  def clearBlockList(id : Long) : Future[StatusCode]
}

object UserService {

  def apply()(implicit ec : ExecutionContext) = new UserService with DatabaseSupport with Logging {

    def login(loginRequest : UserLogin) : Future[LoginSuccess] = {
      for {
        user <- phantomUsersDao.login(loginRequest)
        existingSession <- sessions.existingSession(user.id.get)
        session <- getOrCreateSession(user, existingSession)
      } yield LoginSuccess(session.sessionId)
    }

    def logout(sessionId : String) : Future[Unit] = {
      sessions.removeSession(UUID.fromString(sessionId))
    }

    private def getOrCreateSession(user : PhantomUser, sessionOpt : Option[PhantomSession]) : Future[PhantomSession] = {
      sessionOpt.map(Future.successful).getOrElse(sessions.createSession(PhantomSession.newSession(user)))
    }

    def findById(id : Long) : Future[PhantomUser] = {
      phantomUsersDao.find(id)
    }

    def findContactsById(id : Long) : Future[List[PhantomUser]] = {
      phantomUsersDao.findContacts(id)
    }

    def updateContacts(id : Long, contactList : List[String]) : Future[List[Long]] = {
      val session = db.createSession
      val updatedContacts : Promise[List[Long]] = Promise()

      session.withTransaction {
        val res = for {
          d <- contacts.deleteAll(id)(session)
          ids <- phantomUsers.findPhantomUserIdsByPhone(contactList)
          insert <- contacts.insertAll(ids.map(Contact(None, id, _, "friend")))
        } yield insert

        res.onComplete {
          case Success(contacts : List[Contact]) => {
            updatedContacts.success(contacts.map(_.contactId))
          }
          case Failure(ex) => {
            session.rollback()
            updatedContacts.failure(ex)
          }
        }
      }

      updatedContacts.future
    }

    def clearBlockList(id : Long) : Future[StatusCode] = {
      phantomUsersDao.clearBlockList(id)
    }
  }

}

