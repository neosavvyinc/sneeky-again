package com.phantom.ds.integration.apple

import akka.actor.Actor
import scala.util.{ Success, Failure }
import com.phantom.ds.DSConfiguration
import com.phantom.ds.framework.Logging
import com.phantom.ds.framework.exception.ShoutoutException
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

  private def readPem(location : String) = {
    this.getClass.getClassLoader.getResourceAsStream(location)
  }

  private val environment : ApnsEnvironment = ApplePushConfiguration.environment match {
    case "production" => ApnsEnvironment.getProductionEnvironment
    case _            => ApnsEnvironment.getSandboxEnvironment
  }

  private val certificate : String = ApplePushConfiguration.environment match {
    case "production" => ApplePushConfiguration.productionCert
    case _            => ApplePushConfiguration.developmentCert
  }

  private val keystoreInputStream = readPem(certificate)

  val pushManager = for {
    keyStore <- Try(java.security.KeyStore.getInstance("PKCS12"))
    _ <- Try(keyStore.load(keystoreInputStream, ApplePushConfiguration.keyStorePassword.toCharArray))
    pm <- Try(
      new PushManager[SimpleApnsPushNotification](
        environment,
        keyStore,
        ApplePushConfiguration.keyStorePassword.toCharArray,
        ApplePushConfiguration.connectionCount
      )
    )
    _ <- Try(pm.registerRejectedNotificationListener(new AppleAPNSRejectListener()))
    _ <- Try(pm.start())
    _ <- Try(keystoreInputStream.close())
  } yield pm
}

class AppleActor extends Actor with DSConfiguration with Logging {

  def receive : Actor.Receive = {
    case AppleNotification(shouldPlaySound, token, unviewedMessages, messageText) => {
      log.trace(s"received push notification request with $token")

      val payloadBuilder = new ApnsPayloadBuilder()
      payloadBuilder.setBadgeNumber(unviewedMessages)
      payloadBuilder.setAlertBody(messageText)

      if (shouldPlaySound) {
        log.trace(s"Turning ON sound since user user with $token prefers TO hear sound")
        payloadBuilder.setSoundFileName("default")
      }

      val payload = payloadBuilder.buildWithDefaultMaximumLength()

      AppleService.pushManager match {
        case Failure(ex) => {
          log.trace(s"Error pushing message for $token caused by: $ex")
          throw ShoutoutException.apnsError(ex.toString())
        }
        case Success(pm) => {
          log.trace(s"Successful push for $token")
          token match {
            case Some(t) => pm.enqueuePushNotification(new SimpleApnsPushNotification(TokenUtil.tokenStringToByteArray(t), payload))
            case None    => log.error(s"tried to send push notification via APNS but received an empty token.")
          }
        }
      }
    }
  }
}

case class AppleNotification(shouldPlaySound : Boolean, token : Option[String], unreadMessageCount : Int, messageText : String)
