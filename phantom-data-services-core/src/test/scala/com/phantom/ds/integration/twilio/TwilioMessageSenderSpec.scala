package com.phantom.ds.integration.twilio

import org.specs2.mutable.Specification
import com.phantom.ds.DSConfiguration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TwilioMessageSenderSpec extends Specification with DSConfiguration {

  val sender = TwiioMessageSender(TwilioConfiguration.accountSid, TwilioConfiguration.authToken, TwilioConfiguration.phoneNumber)
  val d = FiniteDuration(5, SECONDS)

  /*"sending SMS messages by way of twilio" should {

    "be able to handle an invalid phone number" in {

      val sms = Await.result(sender.sendInvitation("+15005550001"), d)

      println(s" status of sms is ${sms.getStatus} for ${sms.toString}")
      sms should not be null
    }

  }*/

}