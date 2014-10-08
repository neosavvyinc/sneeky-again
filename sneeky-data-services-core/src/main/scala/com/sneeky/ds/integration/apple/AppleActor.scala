package com.sneeky.ds.integration.apple

import akka.actor.Actor
import scala.util.{ Success, Failure }
import com.sneeky.ds.DSConfiguration
import com.sneeky.ds.framework.Logging
import com.sneeky.ds.framework.exception.ShoutoutException
import scala.util.Try
import com.relayrides.pushy.apns._
import util._

class AppleAPNSRejectListener extends RejectedNotificationListener[SimpleApnsPushNotification] with Logging {

  override def handleRejectedNotification(
    pushManager : PushManager[_ <: SimpleApnsPushNotification], notification : SimpleApnsPushNotification, reason : RejectedNotificationReason) : Unit = {
    log.trace(s"received rejection notification")
    log.trace(s"notification: $notification")
    log.trace(s"reason: $reason")
  }

}

object AppleService extends DSConfiguration {

  private def readPem(location : String) = {
    this.getClass.getClassLoader.getResourceAsStream(location)
  }

  /**
   * We always use a production APNS environment.
   */
  private val environment : ApnsEnvironment = ApnsEnvironment.getProductionEnvironment
  //  ApplePushConfiguration.environment match {
  //    case "production" => ApnsEnvironment.getProductionEnvironment
  //    case _            => ApnsEnvironment.getSandboxEnvironment
  //  }

  private val certificate : String = ApplePushConfiguration.environment match {
    case "production" => ApplePushConfiguration.productionCert
    case _            => ApplePushConfiguration.developmentCert
  }

  private val keystoreInputStream = readPem(certificate)

  val keyStore = java.security.KeyStore.getInstance("PKCS12")
  keyStore.load(keystoreInputStream, ApplePushConfiguration.keyStorePassword.toCharArray)

  val pushManagerFactory = new PushManagerFactory[SimpleApnsPushNotification](
    environment,
    PushManagerFactory.createDefaultSSLContext(
      keyStore,
      ApplePushConfiguration.keyStorePassword.toCharArray)
  )

  val pushManager = pushManagerFactory.buildPushManager()
  pushManager.registerRejectedNotificationListener(new AppleAPNSRejectListener)
  pushManager.start()
  keystoreInputStream.close()
}

class AppleActor extends Actor with DSConfiguration with Logging {

  def receive : Actor.Receive = {
    case AppleNotification(sendPushNotifications, token, unviewedMessages, messageText) => {
      log.trace(s"received push notification request with $token")

      val payloadBuilder = new ApnsPayloadBuilder()
      payloadBuilder.setBadgeNumber(unviewedMessages)
      payloadBuilder.setAlertBody(messageText)
      payloadBuilder.setSoundFileName("default")

      val payload = payloadBuilder.buildWithDefaultMaximumLength()

      if (sendPushNotifications) {

        token match {
          case Some(t) => {
            log.trace(s"Queing the message for $t with $messageText")
            AppleService.pushManager.getQueue().put(new SimpleApnsPushNotification(TokenUtil.tokenStringToByteArray(t), payload))
          }
          case None => log.error(s"tried to send push notification via APNS but received an empty token.")
        }

      } else {

        log.trace(s"No notification was sent for $token because sendPushNotifications was $sendPushNotifications")

      }
    }
  }
}

case class AppleNotification(sendPushNotifications : Boolean, token : Option[String], unreadMessageCount : Int, messageText : String)
