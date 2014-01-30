package com.phantom.ds.integration.twilio

import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.ds.framework.Logging
import com.twilio.sdk.TwilioRestClient
import com.twilio.sdk.resource.instance.Sms
import scala.collection.JavaConversions._
import scala.util.{ Success, Failure, Try }

trait TwilioMessageSender {

  def sendInvitations(phone : Seq[String]) : Future[Seq[Either[TwilioSendFail, Sms]]]
  def sendInvitation(phone : String) : Future[Either[TwilioSendFail, Sms]]
}

object TwiioMessageSender {

  def apply(accountSid : String, authToken : String, phoneNumber : String)(implicit ec : ExecutionContext) = new TwilioMessageSender with Logging {

    val to = "To"
    val from = "From"
    val body = "Body"

    val bodyText = "yo..DL this app!"

    val client = new TwilioRestClient(accountSid, authToken)
    val account = client.getAccount

    def sendInvitations(contacts : Seq[String]) : Future[Seq[Either[TwilioSendFail, Sms]]] = {
      Future.sequence(contacts.map(sendInvitation))
    }

    def sendInvitation(phone : String) : Future[Either[TwilioSendFail, Sms]] = {
      future {
        val response = Try {
          val factory = account.getSmsFactory
          account.getSmsMessages
          val messageParameters = Map(to -> phone, from -> phoneNumber, body -> bodyText)
          factory.create(messageParameters)
        }
        response match {
          case Failure(x) => Left(TwilioSendFail(x))
          case Success(x) => Right(x)
        }
      }
    }
  }

}