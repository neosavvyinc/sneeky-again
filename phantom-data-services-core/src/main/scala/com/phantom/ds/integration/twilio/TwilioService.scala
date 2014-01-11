package com.phantom.ds.integration.twilio

import scala.concurrent.{ ExecutionContext, Future }
import com.phantom.ds.framework.Logging
import com.phantom.ds.DSConfiguration

trait TwilioService {
  //parse the registration response and determine whether or not it is valid
  def verifyRegistration(response : RegistrationVerification) : Future[Unit] // for now
  def sendInvitations(contacts : List[String]) : Future[Unit]
  def recordInvitationStatus(status : InviteMessageStatus) : Future[Unit]
}

object TwilioService {
  def apply(implicit ec : ExecutionContext) : TwilioService = new TwilioService with Logging with DSConfiguration {

    def verifyRegistration(response : RegistrationVerification) : Future[Unit] = {
      log.error(s"received $response")
      val uuidOpt = UUIDExtractor.extractUUID(response)

      //if uuid is present, and in the database, update that record, else log an error?
      Future.successful(Unit)

    }

    // for now..this could probably its own actor w/ a router on it
    def sendInvitations(contacts : List[String]) : Future[Unit] = {
      Future.successful()
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