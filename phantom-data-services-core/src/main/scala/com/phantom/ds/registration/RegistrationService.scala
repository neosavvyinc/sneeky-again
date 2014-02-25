package com.phantom.ds.registration

import com.phantom.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.framework.Logging
import com.phantom.ds.user.Passwords
import java.util.UUID
import com.phantom.model.RegistrationVerification
import com.phantom.model.RegistrationResponse
import com.phantom.model.UserRegistration
import scala.slick.session.Session

trait RegistrationService {

  def register(registrationRequest : UserRegistration) : Future[RegistrationResponse]
  def verifyRegistration(response : RegistrationVerification) : Future[Unit]
}

object RegistrationService {

  def apply()(implicit ec : ExecutionContext) =
    new RegistrationService with DatabaseSupport with Logging {

      def register(registrationRequest : UserRegistration) : Future[RegistrationResponse] = {
        for {
          _ <- Passwords.validate(registrationRequest.password)
          registrationResponse <- doRegistration(registrationRequest)
        } yield registrationResponse
      }

      private def doRegistration(registrationRequest : UserRegistration) : Future[RegistrationResponse] = {
        future {
          db.withTransaction { implicit s =>
            val user = phantomUsersDao.registerOperation(registrationRequest)
            val session = sessions.createSessionOperation(PhantomSession.newSession(user))
            RegistrationResponse(user.uuid, session.sessionId)
          }
        }
      }

      def verifyRegistration(response : RegistrationVerification) : Future[Unit] = {
        val uuidOpt = UUIDExtractor.extractUUID(response)

        log.debug(s"received the uuid: " + uuidOpt)

        uuidOpt.map(updateUserStatus(_, response)).getOrElse(logBadVerification(response))
      }

      private def updateUserStatus(uuid : UUID, message : RegistrationVerification) : Future[Unit] = {
        future {
          db.withTransaction { implicit session : Session =>
            for {
              //TODO: this should be conditional on if the phone number is not already in the database
              verified <- phantomUsersDao.verifyUserOperation(uuid, message.from)
              convertedCount <- convertStubUser(verified, message.from)
            } yield convertedCount
          }
        }
      }

      private def convertStubUser(userId : Long, phoneNumber : String)(implicit session : Session) : Option[Int] = {
        log.trace(s"looking for stub user who has a number: $phoneNumber")
        val stubUserOpt = phantomUsersDao.findMatchingStubUserOperation(phoneNumber)
        stubUserOpt.map { stubUser =>
          log.trace(s"converting stubUser $stubUser")
          val stubUserId = stubUser.id.get
          conversationDao.swapConversationsOperation(stubUserId, userId)
          conversationItemDao.swapConversationItemsOperation(stubUserId, userId)
          phantomUsersDao.deleteOperation(stubUserId)
        }
      }

      private def logBadVerification(response : RegistrationVerification) : Future[Unit] = {
        Future.successful(log.error(s"invalid UUID detected for $response"))
      }

    }
}