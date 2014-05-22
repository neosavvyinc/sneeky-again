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
import com.phantom.ds.BasicCrypto
import scala.slick.session.Session

trait UserService {

  def login(loginRequest : UserLogin) : Future[LoginSuccess]
  def logout(sessionId : String) : Future[Int]
  def updateContacts(id : Long, contacts : List[String]) : Future[List[SanitizedContact]]
  //def blockUsers(id : Long, numbers : Set[String]) : Future[Int]
  def clearBlockList(id : Long) : Future[Int]
  def forgotPassword(email : String) : Future[Boolean]
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

    def findById(id : Long) : Future[PhantomUser] = {
      future {
        db.withSession { implicit session =>
          val opt = phantomUsersDao.findByIdOperation(id)
          opt.getOrElse(throw PhantomException.nonExistentUser)
        }
      }
    }

    def updateContacts(id : Long, contactList : List[String]) : Future[List[SanitizedContact]] = {

      val decryptedContacts : List[String] = contactList.map(c => decryptField(c))
      future {
        db.withTransaction { implicit session : Session =>

          log.trace(s"decrypted : $decryptedContacts")
          //delete all unblocked users
          contacts.deleteAllUnblockedOperation(id)

          //fetch all contacts, which are all the remaining blocked users
          val blockedUsers : List[(Contact, PhantomUser)] = contacts.findAllForOwnerOperation(id)
          log.trace(s"blockedUsers: $blockedUsers")

          //now, lets take the user's uploaded contacts, and filter out any numbers that appear in the blocked list.
          //we do this to avoid inserting any dupes, since blocked users already are in the table
          val filteredDecrypted = decryptedContacts.filter(x => blockedUsers.find(_._2.phoneNumber == Some(x)).isEmpty)
          log.trace(s"filterDecrypted: $filteredDecrypted")

          //resolve the filtered list into phantom users
          val (users, _) = phantomUsersDao.findPhantomUserIdsByPhoneOperation(filteredDecrypted)

          contacts.insertAllOperation(users.map(u => Contact(None, id, u.id.get)))

          //get a list of sanitizedcontacts which represent the user's blocked contacts who they are actually connected to(ie: not a random stranger they blocked who is not in their phone list)
          val blockedContacts = blockedUsers.collect {
            new PartialFunction[(Contact, PhantomUser), SanitizedContact] {
              override def apply(v1 : (Contact, PhantomUser)) : SanitizedContact = SanitizedContact(encryptLocalDate(v1._2.birthday), v1._2.status, encryptOption(v1._2.phoneNumber))

              override def isDefinedAt(x : (Contact, PhantomUser)) : Boolean = decryptedContacts.exists(y => x._2.phoneNumber == Some(y))
            }
          }

          log.trace(s"blockedContacts: $blockedContacts")

          users.map(u => SanitizedContact(
            encryptLocalDate(u.birthday),
            u.status,
            encryptOption(u.phoneNumber)
          )) ++ blockedContacts
        }
      }
    }

    //no use for htis now..but in multi conversations, you might want to delete multiple users..so keeping this in as a reference
    /* def blockUsers(id : Long, numbers : Set[String]) : Future[Int] = {
      future {
        db.withTransaction { implicit session : Session =>
          //find existing contacts whose numbers match
          //change those to Blocked
          //find existing users for the rest
          //insert as blocked
          //note any phone numbers that don't map to users, get ignored
          val existingContacts = contacts.findAllForOwnerInSetOperation(id, numbers)
          val unconnectedContacts = numbers.filterNot(x => existingContacts.exists { case (c, u) => u.phoneNumber == Some(x) })
          val ids = existingContacts.map { case (c, u) => c.id }.flatten.toSet
          contacts.blockContactsOperation(ids)
          val (users, _) = phantomUsersDao.findPhantomUserIdsByPhoneOperation(unconnectedContacts.toList)
          val inserted = contacts.insertAllOperation(users.map(x => Contact(None, id, x.id.get, Blocked)))
          existingContacts.size + inserted.size
        }
      }
    }*/

    def clearBlockList(id : Long) : Future[Int] = {
      future {
        db.withTransaction { implicit session =>
          contacts.clearBlockListOperation(id)
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
        status <- resetPassword(email)
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

