package com.phantom.ds.integration.apple

import akka.actor.Actor
import scala.util.{ Success, Failure }
import com.phantom.ds.DSConfiguration
import com.phantom.ds.framework.Logging
import com.phantom.model.PhantomUser
import com.phantom.ds.framework.exception.PhantomException
import scala.util.Try
import com.relayrides.pushy.apns._
import util._
import java.io.BufferedInputStream
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream

class AppleAPNSRejectListener extends RejectedNotificationListener[SimpleApnsPushNotification] with Logging {
  def handleRejectedNotification(notification : SimpleApnsPushNotification, reason : RejectedNotificationReason) = {
    log.trace(s"received rejection notification")
    log.trace(s"notification: $notification")
    log.trace(s"reason: $reason")
  }
}

object AppleService extends DSConfiguration {

  private def readPem(location : String) = {
    val in4 = this.getClass.getClassLoader.getResourceAsStream(location)
    in4
  }

  private val keystoreInputStream = readPem(ApplePushConfiguration.certPath)

  val pushManager = for {
    keyStore <- Try(java.security.KeyStore.getInstance("PKCS12"))
    _ <- Try(keyStore.load(keystoreInputStream, ApplePushConfiguration.keyStorePassword.toCharArray()))
    pm <- Try(
      new PushManager[SimpleApnsPushNotification](
        ApnsEnvironment.getSandboxEnvironment(),
        keyStore,
        ApplePushConfiguration.keyStorePassword.toCharArray()
      )
    )
    _ <- Try(pm.registerRejectedNotificationListener(new AppleAPNSRejectListener()))
    _ <- Try(pm.start())
    _ <- Try(keystoreInputStream.close())
  } yield pm
}

class AppleActor extends Actor with DSConfiguration with Logging {

  def receive : Actor.Receive = {
    case token : String => {
      log.trace(s"received $token")
      //val tokenString = "b4aa9a5aa1ac55ac0c038b8c55733e90b68290592ae1d76dd2d0837e38bfb0da" // chris

      val payloadBuilder = new ApnsPayloadBuilder()
      payloadBuilder.setBadgeNumber(1)
      payloadBuilder.setAlertBody(ApplePushConfiguration.messageBody)
      payloadBuilder.setSoundFileName("default")

      val payload = payloadBuilder.buildWithDefaultMaximumLength()

      AppleService.pushManager match {
        case Failure(ex) => throw PhantomException.apnsError(ex.toString())
        case Success(pm) => pm.enqueuePushNotification(
          new SimpleApnsPushNotification(TokenUtil.tokenStringToByteArray(token), payload)
        )
      }
    }
  }
}

sealed trait AppleMessage
