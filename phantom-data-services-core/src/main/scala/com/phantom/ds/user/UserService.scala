package com.phantom.ds.user

import scala.concurrent.{ Promise, ExecutionContext, Future, future }
import scala.util.{ Success, Failure }
import spray.http.StatusCode
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
  def updateContacts(id : Long, contacts : List[String]) : Future[List[Contact]]
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

    def updateContacts(id : Long, contactList : List[String]) : Future[List[Contact]] = {
      val session = db.createSession
      val updatedContacts : Promise[List[Contact]] = Promise()

      future {
        // TO DO
        // need to partition phone number request, update contacts for all
        // users that exist, then take numbersNotFound and create stub users
        session.withTransaction {
          val res = for {
            d <- contacts.deleteAll(id)(session)
            (ids : List[Long], numbersNotFound : List[String]) <- phantomUsersDao.findPhantomUserIdsByPhone(contactList)
            bogus <- future { println(numbersNotFound) } // do something with this list, create stub users???
            insert <- contacts.insertAll(ids.map(Contact(None, id, _, "friend")))
          } yield insert

          res.onComplete {
            case Success(contacts : List[Contact]) => updatedContacts.success(contacts)
            case Failure(ex) => {
              session.rollback()
              updatedContacts.failure(ex)
            }
            // compiler complains of non-exhaustive pattern match without this. can we ever hit this though?
            case _ => updatedContacts.failure(new Exception("unidentified exception : updateContacts"))
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

