package com.phantom.ds.integration.twilio

import akka.actor.Actor
import com.phantom.ds.DSConfiguration
import com.phantom.ds.framework.Logging
import com.phantom.model.PhantomUser
import scala.concurrent.ExecutionContext

class TwilioActor(service : TwilioService)(implicit ec : ExecutionContext) extends Actor with DSConfiguration with Logging {

  def receive : Actor.Receive = {
    case x : SendInviteToStubUsers => handleInviteStubUsers(x)
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

case class SendInviteToStubUsers(stubUsers : Seq[PhantomUser], tries : Int = 0) extends TwilioMessage
