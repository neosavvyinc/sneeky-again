package com.phantom.ds.integration.twilio

import akka.actor.Actor
import com.phantom.ds.DSConfiguration
import com.phantom.ds.framework.Logging
import com.phantom.model.PhantomUser
import scala.concurrent.ExecutionContext

class TwilioActor(service : TwilioService)(implicit ec : ExecutionContext) extends Actor with DSConfiguration with Logging {

  def receive : Actor.Receive = {
    case x : SendInvite            => handleInviteUnidentifiedContacts(x)
    case x : SendInviteToStubUsers => handleInviteStubUsers(x)
    case x : InviteMessageStatus   => service.recordInvitationStatus(x)
  }

  private def handleInviteUnidentifiedContacts(msg : SendInvite) = {
    val failedSendsF = service.sendInvitationsToUnidentifiedUsers(msg)
    /*failedSendsF.onSuccess {
      case x if msg.tries < UserConfiguration.maxRetries => self ! msg.copy(contacts = x.toSet, tries = msg.tries + 1)
    }*/
    failedSendsF.onFailure {
      case t : Throwable => log.error(t.getMessage, t)
    }
  }

  private def handleInviteStubUsers(msg : SendInviteToStubUsers) = {
    val failedSendsF = service.sendInvitationsToStubUsers(msg.stubUsers)
    /*failedSendsF.onSuccess {
      case x if msg.tries < UserConfiguration.maxRetries => self ! SendInviteToStubUsers(x, msg.tries + 1)
    }*/
    //TODO ROBUST THIS FUNCTION IN THE FACE
    failedSendsF.onFailure {
      case t : Throwable => log.error(t.getMessage, t)
    }
  }
}

sealed trait TwilioMessage

case class SendInvite(contacts : Set[String], from : Long, imageText : String, imageUrl : String, tries : Int = 0) extends TwilioMessage

case class SendInviteToStubUsers(stubUsers : Seq[PhantomUser], tries : Int = 0) extends TwilioMessage

case class InviteMessageStatus(messageSid : String, status : String) extends TwilioMessage
