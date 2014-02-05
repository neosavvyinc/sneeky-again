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
          user <- phantomUsersDao.register(registrationRequest)
          session <- sessions.createSession(PhantomSession.newSession(user))
        } yield RegistrationResponse(user.uuid, session.sessionId)
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
              verified <- phantomUsersDao.verifyUserOperation(uuid, message.from)
              convertedCount <- convertStubUser(verified, message.from)
            } yield convertedCount
          }
        }
      }

      private def convertStubUser(userId : Long, phoneNumber : String)(implicit session : Session) : Option[Int] = {
        val stubUserOpt = stubUsersDao.findByPhoneNumberOperation(phoneNumber)
        stubUserOpt.map { stubUser =>
          val stubConversations = stubConversationsDao.findByToStubUserIdOperation(stubUser.id.get)
          val realConversations = conversationDao.insertAllOperation(stubConversationtoReal(stubConversations, userId))
          conversationItemDao.insertAllOperation(stubConversationsToItems(stubConversations.zip(realConversations)))
          stubUsersDao.deleteOperation(stubUser.id.get)
          stubConversationsDao.deleteOperation(stubConversations.map(_.id.get))
        }
      }

      private def stubConversationtoReal(conversations : Seq[StubConversation], toUserId : Long) : Seq[Conversation] = {
        conversations.map(x => Conversation(None, toUserId, x.fromUser))
      }

      private def stubConversationsToItems(paired : Seq[(StubConversation, Conversation)]) : Seq[ConversationItem] = {
        paired.map {
          case (stub, real) =>
            ConversationItem(None, real.id.get, stub.imageUrl, stub.imageText)
        }
      }

      private def logBadVerification(response : RegistrationVerification) : Future[Unit] = {
        Future.successful(log.error(s"invalid UUID detected for $response"))
      }

    }
}