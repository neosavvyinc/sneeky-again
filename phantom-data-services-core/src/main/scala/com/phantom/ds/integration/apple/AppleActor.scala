package com.phantom.ds.integration.apple

import akka.actor.Actor
import scala.util.{ Success, Failure }
import com.phantom.ds.DSConfiguration
import com.phantom.ds.framework.Logging
import com.phantom.model.PhantomUser
import scala.util.Try
import com.relayrides.pushy.apns._
import util._

class AppleAPNSRejectListener extends RejectedNotificationListener[SimpleApnsPushNotification] with Logging {
  def handleRejectedNotification(notification : SimpleApnsPushNotification, reason : RejectedNotificationReason) = {
    log.trace(s"received rejection notification")
    log.trace(s"notification: $notification")
    log.trace(s"reason: $reason")
  }
}

object AppleService extends DSConfiguration {

  private val keystoreInputStream = new java.io.FileInputStream(ApplePushConfiguration.certPath)

  private val keyStore = for {
    ks <- Try(java.security.KeyStore.getInstance("PKCS12"))
    _ <- Try(ks.load(keystoreInputStream, ApplePushConfiguration.keyStorePassword.toCharArray()))
  } yield ks

  // TO DO
  // Figure out some sort of "finally" syntax to always
  // close keystore stream and/or error handling
  // Try(keystoreInputStream.close())

  //    keyStore match {
  //      case Failure(ex) => {
  //        // throw some exception here related to certs
  //        println("Exception", ex.toString)
  //        keystoreInputStream.close()
  //      }
  //      case Success(s) => println("Success")
  //    }
  //
  val pushManager = new PushManager[SimpleApnsPushNotification](
    ApnsEnvironment.getSandboxEnvironment(), // Production v. Sandbox
    keyStore.get, // FIX THIS
    ApplePushConfiguration.keyStorePassword.toCharArray()
  )

  private val listener = new AppleAPNSRejectListener()

  pushManager.registerRejectedNotificationListener(listener)
  pushManager.start()

  // finally?
  keystoreInputStream.close()
}

class AppleActor extends Actor with Logging {

  def receive : Actor.Receive = {
    case user : PhantomUser => {
      log.trace(s"received $user")
      val tokenString = "b4aa9a5aa1ac55ac0c038b8c55733e90b68290592ae1d76dd2d0837e38bfb0da" // chris

      val payloadBuilder = new ApnsPayloadBuilder()
      payloadBuilder.setBadgeNumber(1)
      payloadBuilder.setAlertBody("you got a new dick pic!")
      payloadBuilder.setSoundFileName("dicks-on-the-phone-rang-rang.aiff")

      val payload = payloadBuilder.buildWithDefaultMaximumLength()

      AppleService.pushManager.enqueuePushNotification(
        new SimpleApnsPushNotification(TokenUtil.tokenStringToByteArray(tokenString), payload))
    }
  }
}

sealed trait AppleMessage

case class SendConversationNotification(users : Seq[PhantomUser]) // for now
