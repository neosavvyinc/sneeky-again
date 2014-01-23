package com.phantom.ds.integration.twilio

import akka.actor.Actor
import com.phantom.ds.DSConfiguration
import com.phantom.ds.framework.Logging

class TwilioActor(service : TwilioService) extends Actor with DSConfiguration with Logging {

  def receive : Actor.Receive = {
    case SendInvite(contacts)         => service.sendInvitations(contacts)
    case x : RegistrationVerification => service.verifyRegistration(x)
    case x : InviteMessageStatus      => service.recordInvitationStatus(x)
  }
}

sealed trait TwilioMessage

case class SendInvite(contacts : List[String]) extends TwilioMessage

case class InviteMessageStatus(messageSid : String, status : String) extends TwilioMessage

case class RegistrationVerification(messageSid : String,
                                    accountSid : String,
                                    from : String,
                                    to : String,
                                    body : String,
                                    numMedia : Int) extends TwilioMessage

