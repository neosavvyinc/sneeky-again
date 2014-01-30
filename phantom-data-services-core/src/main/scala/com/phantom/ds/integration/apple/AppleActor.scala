package com.phantom.ds.integration.apple

import akka.actor.Actor
import com.phantom.ds.DSConfiguration
import com.phantom.ds.framework.Logging
import com.phantom.model.PhantomUser

class AppleActor extends Actor with DSConfiguration with Logging {

  def receive : Actor.Receive = {
    case x => log.trace(s"received $x")
  }

}

sealed trait AppleMessage

case class SendConversationNotification(users : Seq[PhantomUser]) // for now