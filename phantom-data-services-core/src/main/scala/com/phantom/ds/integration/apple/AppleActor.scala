package com.phantom.ds.integration.apple

import akka.actor.Actor
import com.phantom.ds.DSConfiguration
import com.phantom.ds.framework.Logging
import com.phantom.model.PhantomUser
import scala.util.Try
import com.relayrides.pushy.apns._
import util._

// http://relayrides.github.io/pushy/

trait AppleService {
  def pushManager : PushManager[SimpleApnsPushNotification]
}

object AppleService {
  def apply(config : String) : AppleService = new AppleService {

    private val keyStorePassword = "KEY_STORE_PASS"
    private val certPath = "/path/to/certificate.p12"
    private val keystoreInputStream = new java.io.FileInputStream(certPath)

    val keyStore = for {
      keyStore <- Try(java.security.KeyStore.getInstance("PKCS12"))
      _ <- Try(keyStore.load(keystoreInputStream, keyStorePassword.toCharArray()))
    } yield keyStore

    // TO DO
    // Figure out some sort of "finally" syntax to always
    // close keystore stream
    // Try(keystoreInputStream.close())

    def pushManager = new PushManager[SimpleApnsPushNotification](
      ApnsEnvironment.getSandboxEnvironment(), // Production v. Sandbox
      keyStore.getOrElse(throw new Exception()), // FIX THIS
      keyStorePassword.toCharArray()
    )

    // finally?
    keystoreInputStream.close()

    pushManager.start()
  }
}

class AppleActor(service : AppleService) extends Actor with DSConfiguration with Logging {

  def receive : Actor.Receive = {
    case x => {
      log.trace(s"received $x")
      val tokenString = "<5f6aa01d 8e335894 9b7c25d4 61bb78ad 740f4707 462c7eaf bebcf74f a5ddb387>"
      val token = TokenUtil.tokenStringToByteArray(tokenString)

      val payloadBuilder = new ApnsPayloadBuilder()
      payloadBuilder.setBadgeNumber(1)
      payloadBuilder.setAlertBody("you got a new dick pic!")
      payloadBuilder.setSoundFileName("dicks-on-the-phone-rang-rang.aiff")

      val payload = payloadBuilder.buildWithDefaultMaximumLength()
      service.pushManager.enqueuePushNotification(new SimpleApnsPushNotification(token, payload))
    }
  }

}

sealed trait AppleMessage

case class SendConversationNotification(users : Seq[PhantomUser]) // for now
