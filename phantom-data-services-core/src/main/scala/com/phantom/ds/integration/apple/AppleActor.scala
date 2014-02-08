package com.phantom.ds.integration.apple

import akka.actor.Actor
import scala.util.{ Success, Failure }
import com.phantom.ds.DSConfiguration
import com.phantom.ds.framework.Logging
import com.phantom.model.PhantomUser
import scala.util.Try
import com.relayrides.pushy.apns._
import util._

trait AppleService {
  def pushManager : PushManager[SimpleApnsPushNotification]
}

object AppleService {
  def apply(keyStorePassword : String, certificatePath : String) : AppleService = new AppleService {

    private val keystoreInputStream = new java.io.FileInputStream(certificatePath)

    private val keyStore = for {
      ks <- Try(java.security.KeyStore.getInstance("PKCS12"))
      _ <- Try(ks.load(keystoreInputStream, keyStorePassword.toCharArray()))
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
    def pushManager = new PushManager[SimpleApnsPushNotification](
      ApnsEnvironment.getSandboxEnvironment(), // Production v. Sandbox
      keyStore.getOrElse(throw new Exception()), // FIX THIS
      keyStorePassword.toCharArray()
    )

    pushManager.start()

    // finally?
    keystoreInputStream.close()
  }
}

class AppleActor(service : AppleService) extends Actor with DSConfiguration with Logging {

  def receive : Actor.Receive = {
    case user : PhantomUser => {
      log.trace(s"received $user")
      val tokenString = "6964897bb4b06ea805ae58b7821c3cfab77e34347785bd06f6f22d0e07dea176"
      //val tokenString = "<5f6aa01d 8e335894 9b7c25d4 61bb78ad 740f4707 462c7eaf bebcf74f a5ddb387>"

      val payloadBuilder = new ApnsPayloadBuilder()
      payloadBuilder.setBadgeNumber(1)
      payloadBuilder.setAlertBody("you got a new dick pic!")
      payloadBuilder.setSoundFileName("dicks-on-the-phone-rang-rang.aiff")

      val payload = payloadBuilder.buildWithDefaultMaximumLength()
      service.pushManager.enqueuePushNotification(
        new SimpleApnsPushNotification(TokenUtil.tokenStringToByteArray(tokenString), payload))
    }
  }

}

sealed trait AppleMessage

case class SendConversationNotification(users : Seq[PhantomUser]) // for now
