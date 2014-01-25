package com.phantom.ds.integration.twilio

import org.specs2.mutable.Specification
import com.phantom.ds.dataAccess.BaseDAOSpec
import scala.concurrent.ExecutionContext.Implicits.global
import com.phantom.model.Verified
import com.phantom.ds.TestUtils
import scala.concurrent.Await
import scala.concurrent.duration._

class TwilioServiceSpec extends Specification
    with BaseDAOSpec
    with TestUtils {

  private val svc = TwilioService(TwiioMessageSender(TwilioConfiguration.accountSid, TwilioConfiguration.authToken, TwilioConfiguration.phoneNumber))(global)

  sequential

}