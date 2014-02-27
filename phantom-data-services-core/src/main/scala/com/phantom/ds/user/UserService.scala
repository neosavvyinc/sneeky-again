package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.model._
import com.phantom.ds.framework.Logging
import com.phantom.model.UserLogin
import com.phantom.model.PhantomUser
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.ds.framework.email.{ MandrillConfiguration, MandrillUtil }

trait UserService {

  def login(loginRequest : UserLogin) : Future[LoginSuccess]
  def logout(sessionId : String) : Future[Int]
  def updateContacts(id : Long, contacts : List[String]) : Future[List[SanitizedContact]]
  def clearBlockList(id : Long) : Future[Int]
  def forgotPassword(email : String) : Future[Boolean]
}

object UserService {

  def apply()(implicit ec : ExecutionContext) = new UserService with DatabaseSupport with Logging {

    def login(loginRequest : UserLogin) : Future[LoginSuccess] = {
      for {
        user <- phantomUsersDao.login(loginRequest)
        session <- sessions.createSession(PhantomSession.newSession(user))
      } yield LoginSuccess(session.sessionId)
    }

    def logout(sessionId : String) : Future[Int] = {
      sessions.removeSession(UUID.fromString(sessionId))
    }

    def findById(id : Long) : Future[PhantomUser] = {
      future {
        val opt = phantomUsersDao.find(id)
        opt.getOrElse(throw PhantomException.nonExistentUser)
      }
    }

    //TODO FIX ME..I DELETE BLOCKED USERS
    def updateContacts(id : Long, contactList : List[String]) : Future[List[SanitizedContact]] = {
      val session = db.createSession

      future {
        contacts.deleteAll(id)(session)
        val (users : List[PhantomUser], numbersNotFound : List[String]) = phantomUsersDao.findPhantomUserIdsByPhone(contactList)
        contacts.insertAll(users.map(u => Contact(None, id, u.id.get)))

        users.map(u => SanitizedContact(
          u.birthday,
          u.status,
          u.phoneNumber)
        )
      }
    }

    def clearBlockList(id : Long) : Future[Int] = {
      future {
        db.withTransaction { implicit session =>
          phantomUsersDao.clearBlockListOperation(id)
        }

      }
    }

    def findFromSessionId(sessionId : String) : Future[PhantomSession] = {
      sessions.sessionByUUID(UUID.fromString(sessionId))
    }

    def updatePushNotifier(sessionUUID : UUID, applePushToken : String, mobilePushType : MobilePushType) : Future[Boolean] = {
      future {
        sessions.updatePushNotifier(sessionUUID, applePushToken, mobilePushType)
      }
    }

    def updateSetting(userId : Long, pushSettingType : SettingType, value : Boolean) : Future[Boolean] = {
      future {
        phantomUsersDao.updateSetting(userId, pushSettingType, value)
      }
    }

    def forgotPassword(email : String) : Future[Boolean] = {
      for {
        status  <- resetPassword(email)
        results <- sendResetPasswordEmail(status)
      } yield results

    }

    private def resetPassword(email : String) : Future[Option[ResetPasswordResults]] = {
      future {
        db.withTransaction { implicit session =>
          val userOpt = phantomUsersDao.findByEmailOperation(email)
          userOpt.map { user =>
            val newPassword = Passwords.generateNewPassword().substring(0, 8)
            val encrypted = Passwords.getSaltedHash(newPassword)
            phantomUsersDao.updatePasswordForUserOperation(email, encrypted)
            val invalidatedSessions = sessions.invalidateAllForUser(user.id.get)
            log.trace(s"There were $invalidatedSessions invalidated for user by email: $email ")
            ResetPasswordResults(email, newPassword)
          }
        }
      }
    }

    //TODO: actor me..why?  well..why not?  this thread does not need to wait or concern itself at all w/ the email sending
    //it should very much be treated identically to an apple push or a twiio notification
    private def sendResetPasswordEmail(status : Option[ResetPasswordResults]) : Future[Boolean] = {
      status match {
        case None => Future.successful(false)
        case Some(x) => {
          future {
            MandrillUtil.sendMailViaMandrill(
              new MandrillConfiguration(
                MandrillConfiguration.apiKey,
                MandrillConfiguration.smtpPort,
                MandrillConfiguration.smtpHost,
                MandrillConfiguration.username
              ), x.email, x.password)
            true
          }
        }
      }
    }

    private[this] case class ResetPasswordResults(email : String, password : String)
  }

}

