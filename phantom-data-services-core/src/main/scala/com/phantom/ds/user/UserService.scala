package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future }
import spray.http.{ StatusCode, StatusCodes }
import com.phantom.model._
import com.phantom.ds.framework.Logging
import com.phantom.model.UserLogin
import com.phantom.model.PhantomUser
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID

trait UserService {

  def login(loginRequest : UserLogin) : Future[LoginSuccess]
  def logout(sessionId : String) : Future[Unit]
  def findById(id : Long) : Future[PhantomUser]
  def findContactsById(id : Long) : Future[List[PhantomUser]]
  def updateContacts(id : Long, contacts : String) : Future[StatusCode]
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
      phantomUsersDao.clearBlockList(id)
    }
  }

}

