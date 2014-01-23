package com.phantom.ds.integration.twilio

import scala.concurrent.{ ExecutionContext, Future }
import com.phantom.ds.framework.Logging
import com.phantom.ds.DSConfiguration
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID

trait TwilioService {
  def verifyRegistration(response : RegistrationVerification) : Future[Unit]
  def sendInvitations(contacts : List[String])
  def recordInvitationStatus(status : InviteMessageStatus) : Future[Unit]
}

object TwilioService {
  def apply(sender : TwilioMessageSender)(implicit ec : ExecutionContext) : TwilioService =

    new TwilioService with DatabaseSupport with Logging with DSConfiguration {

      def verifyRegistration(response : RegistrationVerification) : Future[Unit] = {
        log.error(s"received $response")
        val uuidOpt = UUIDExtractor.extractUUID(response)
        uuidOpt.map(updateUserStatus(_, response)).getOrElse(logBadVerification(response))
      }

      private def updateUserStatus(uuid : UUID, message : RegistrationVerification) : Future[Unit] = {
        val updated = phantomUsers.verifyUser(uuid)
        updated.map { x =>
          if (x != 1) {
            log.error(s"uuid : $uuid extracted from $message is either not valid, or the user is already verified.")
          }
        }
      }

      private def logBadVerification(response : RegistrationVerification) : Future[Unit] = {
        Future.successful(log.error(s"invalid UUID detected for $response"))
      }

      // for now..this could probably its own actor w/ a router on it
      def sendInvitations(contacts : List[String]) {
        val messageStatuses = Future.sequence(contacts.map(sender.sendInvitation))
        messageStatuses.onSuccess {
          case x =>
            x.foreach { message =>
              log.info(s"invitation sent status $message")
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
    }
}