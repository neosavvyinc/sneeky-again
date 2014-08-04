package com.phantom.ds.integration.twilio

import com.twilio.sdk.TwilioRestException

trait TwilioSendFail {

  def code : Int
}

object TwilioSendFail {

  def apply(exception : Throwable) = exception match {
    case x : TwilioRestException => matchTwilioException(x.getErrorCode)
    case x : Throwable           => NonTwilioException(x)
  }

  private def matchTwilioException(code : Int) = code match {
    case 21211 => InvalidNumber
    case 21612 => CannotRoute
    case 21408 => I18nPermissions
    case 21610 => Blacklisted
    case 21614 => SmsUnable
    case x     => Unrecognized(x)
  }
}

case object InvalidNumber extends TwilioSendFail {
  def code : Int = 21211
}

case object CannotRoute extends TwilioSendFail {
  def code : Int = 21612
}

case object I18nPermissions extends TwilioSendFail {
  def code : Int = 21408
}

case object Blacklisted extends TwilioSendFail {
  def code : Int = 21610
}

case object SmsUnable extends TwilioSendFail {
  def code : Int = 21614
}

case class Unrecognized(code : Int) extends TwilioSendFail

case class NonTwilioException(e : Throwable, code : Int = -1) extends TwilioSendFail
