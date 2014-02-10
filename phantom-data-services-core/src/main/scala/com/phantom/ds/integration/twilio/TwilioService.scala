package com.phantom.ds.integration.twilio

import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.ds.framework.Logging
import com.phantom.ds.DSConfiguration
import com.phantom.dataAccess.DatabaseSupport
import com.twilio.sdk.resource.instance.Sms
import com.phantom.model._
import com.phantom.model.Conversation
import java.util.UUID

trait TwilioService {
  def sendInvitationsToUnidentifiedUsers(invite : SendInvite) : Future[Seq[String]]
  def sendInvitationsToStubUsers(stubUsers : Seq[PhantomUser]) : Future[Seq[PhantomUser]]
  def recordInvitationStatus(status : InviteMessageStatus) : Future[Unit]
}

object TwilioService {
  def apply(sender : TwilioMessageSender)(implicit ec : ExecutionContext) : TwilioService =

    new TwilioService with DatabaseSupport with Logging with DSConfiguration {

      def sendInvitationsToUnidentifiedUsers(invite : SendInvite) : Future[Seq[String]] = {
        val seqd = invite.contacts.toSeq
        val resultsF = sender.sendInvitations(seqd).map(x => toResults(seqd.zip(x)))
        for {
          results <- resultsF
          _ <- createStubAccounts(results.passed, invite.from, invite.imageText, invite.imageUrl)
        } yield results.failed
      }

      //not sure how to handle failed known stub users...in theory..they shouldn't exist
      //blacklisted might be the only real case to look out for
      def sendInvitationsToStubUsers(stubUsers : Seq[PhantomUser]) : Future[Seq[PhantomUser]] = {
        val resultsF = sender.sendInvitations(stubUsers.map(_.phoneNumber).flatten).map(x => toResults(stubUsers.zip(x)))

        for {
          results <- resultsF
          _ <- updateInvitationCount(results.passed)
        } yield results.failed
      }

      private def updateInvitationCount(stubUsers : Seq[PhantomUser]) : Future[Int] = {
        phantomUsersDao.updateInvitationCount(stubUsers)
      }

      private def createStubAccounts(contacts : Seq[String], fromUser : Long, imageText : String, imageUrl : String) : Future[Seq[PhantomUser]] = {
        val stagedStubs = contacts.map(x => PhantomUser(None, UUID.randomUUID, None, None, None, false, Some(x), None, None, 1, Stub))
        future {
          db.withTransaction { implicit session =>
            val stubUsers = phantomUsersDao.insertAllOperation(stagedStubs)
            val stagedConversations = stubUsers.map(x => Conversation(None, fromUser, x.id.get))
            val createdConversations = conversationDao.insertAllOperation(stagedConversations)
            conversationItemDao.insertAllOperation(createdConversations.map(x => ConversationItem(None, x.id.get, imageUrl, imageText)))
            stubUsers
          }
        }
      }

      //TODO FIX ME..i do nothing good
      def recordInvitationStatus(status : InviteMessageStatus) : Future[Unit] = {
        Future.successful {
          if (status.status == "failed") {
            log.error(s"failed to send invitation : $status") //note..this message kinda blows..we'd have to call twilio's API to get more info
          }
        }
      }

      private def toResults[T](seq : Seq[(T, Either[TwilioSendFail, Sms])]) : Results[T] = {
        seq.foldLeft(Results[T]()) { (acc, i) =>
          i match {
            case (x, Left(y : NonTwilioException)) => acc.copy(failed = acc.failed :+ x)
            case (x, Left(_))                      => acc.copy(twilioRejected = acc.twilioRejected :+ x)
            case (x, Right(_))                     => acc.copy(passed = acc.passed :+ x)
          }
        }
      }
    }
}

case class Results[T](failed : Seq[T] = Seq(), twilioRejected : Seq[T] = Seq(), passed : Seq[T] = Seq())