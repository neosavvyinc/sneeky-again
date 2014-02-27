package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.model._
import com.phantom.ds.framework.Logging
import com.phantom.model.UserLogin
import com.phantom.model.PhantomUser
import com.phantom.model.SanitizedUser
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID
import com.phantom.ds.framework.exception.PhantomException
import com.microtripit.mandrillapp.lutung.MandrillApi
import com.microtripit.mandrillapp.lutung.view.{ MandrillMessage, MandrillMessageStatus }
import java.util
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.Recipient
import com.phantom.ds.framework.email.{ MandrillConfiguration, MandrillUtil }
import com.phantom.ds.BasicCrypto

trait UserService {

  def login(loginRequest : UserLogin) : Future[LoginSuccess]
  def logout(sessionId : String) : Future[Int]
  def updateContacts(id : Long, contacts : List[String]) : Future[List[SanitizedContact]]
  def clearBlockList(id : Long) : Future[Int]
}

object UserService extends BasicCrypto {

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

    private def getOrCreateSession(user : PhantomUser, sessionOpt : Option[PhantomSession]) : Future[PhantomSession] = {
      sessionOpt.map(Future.successful).getOrElse(sessions.createSession(PhantomSession.newSession(user)))
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

      val decryptedContacts : List[String] = contactList.map(c => decryptField(c))

      future {
        contacts.deleteAll(id)(session)
        val (users : List[PhantomUser], numbersNotFound : List[String]) = phantomUsersDao.findPhantomUserIdsByPhone(decryptedContacts)
        contacts.insertAll(users.map(u => Contact(None, id, u.id.get)))

        users.map(u => SanitizedContact(
          u.birthday,
          u.status,
          encryptOption(u.phoneNumber)
        ))
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
      future {
        //make sure the user has an account
        //val user = phantomUsersDao.findByEmail(email)
        // make sure above line

        // generate a new password with MD5
        val newPassword = Passwords.generateNewPassword().substring(0, 8)
        log.debug(s"Reseting password for user with email $email as password $newPassword")

        // update the user record with the new password
        val passwordEncrypted = Some(Passwords.getSaltedHash(newPassword))
        val passwordUpdated = phantomUsersDao.updatePasswordForUser(email, passwordEncrypted.get)

        // send them an email with the mandrill client
        MandrillUtil.sendMailViaMandrill(
          new MandrillConfiguration(
            MandrillConfiguration.apiKey,
            MandrillConfiguration.smtpPort,
            MandrillConfiguration.smtpHost,
            MandrillConfiguration.username
          ), email, newPassword)

        // mark all the sessions as INVALIDATED
        val numSessionsInvalidated = for {
          activeUser <- phantomUsersDao.findUser(email)
          numSessionsInvalidated <- sessions.invalidateAllForUser(activeUser.id.get)
        } yield numSessionsInvalidated
        log.trace(s"There were $numSessionsInvalidated invalidated for user by email: $email ")

        // if anything fails return the code for each failure scenario
        passwordUpdated
      }
    }
  }

}

