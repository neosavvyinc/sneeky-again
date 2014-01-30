package com.phantom.ds.integration.twilio

import scala.concurrent.{ ExecutionContext, Future }
import com.phantom.ds.framework.Logging
import com.phantom.ds.DSConfiguration
import com.phantom.dataAccess.DatabaseSupport
import com.twilio.sdk.resource.instance.Sms
import com.phantom.model.{ StubConversation, StubUser }

trait TwilioService {
  def sendInvitationsToUnidentifiedUsers(invite : SendInvite) : Future[Seq[String]]
  def sendInvitationsToStubUsers(stubUsers : Seq[StubUser]) : Future[Seq[StubUser]]
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
      def sendInvitationsToStubUsers(stubUsers : Seq[StubUser]) : Future[Seq[StubUser]] = {
        val resultsF = sender.sendInvitations(stubUsers.map(_.phoneNumber)).map(x => toResults(stubUsers.zip(x)))

        for {
          results <- resultsF
          _ <- updateInvitationCount(results.passed)
        } yield results.failed
      }

      private def updateInvitationCount(stubUsers : Seq[StubUser]) : Future[Unit] = {
        stubUsersDao.updateInvitationCount(stubUsers)
      }

      private def createStubAccounts(contacts : Seq[String], fromUser : Long, imageText : String, imageUrl : String) : Future[Seq[StubUser]] = {
        val stagedStubs = contacts.map(x => StubUser(None, x, 1))
        for {
          stubUsers <- stubUsersDao.insertAll(stagedStubs)
          _ <- createStubConversations(stubUsers, fromUser, imageText, imageUrl)
        } yield stubUsers
      }

      private def createStubConversations(stubUsers : Seq[StubUser], fromUser : Long, imageText : String, imageUrl : String) : Future[Seq[StubConversation]] = {
        val stagedConversations = stubUsers.map(x => StubConversation(None, fromUser, x.id.get, imageText, imageUrl))
        stubConversationsDao.insertAll(stagedConversations)
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