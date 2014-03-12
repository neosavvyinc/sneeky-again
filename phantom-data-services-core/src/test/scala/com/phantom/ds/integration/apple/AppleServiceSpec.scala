package com.phantom.ds.integration.apple

import org.specs2.mutable.Specification
import com.phantom.ds.dataAccess.BaseDAOSpec
import com.phantom.ds.TestUtils
import com.relayrides.pushy.apns._
import util._
import scala.util.{ Success, Failure }

class AppleServiceSpec extends Specification
    with BaseDAOSpec
    with TestUtils {

  sequential

  "The Apple Service" should {

    "fire off requests and shit" in withSetupTeardown {

      val tokens = List(
        "b7fd0dc754266b171f0626a5aa733858b8d6387978ca80c7e7e34e1e18962200",
        "f6fa54fbf0504dec17cd855449714cf0c664820bf7b77cec6909cf79c89e2510",
        "e8c79f28c012cdd973ec332e663341f12ff9cd4518f6f380232d75a808804a4e",
        "1fe6e0071068f5f3f048b703b5024dca51b535d682ca9d336d9f7c5e867ebecc",
        "9aa493ba8e8f2b3e403b2c8b8535f4d511693b901c5d61a3f13b9891e6532850"
      )

      val payloadBuilder = new ApnsPayloadBuilder()
      payloadBuilder.setBadgeNumber(1)
      payloadBuilder.setAlertBody(ApplePushConfiguration.messageBody)

      val payload = payloadBuilder.buildWithDefaultMaximumLength()

      val appleService = AppleService.pushManager.get
      Thread.sleep(600)

      //      AppleService.pushManager match {
      //        case Failure(ex) => {
      //          log.trace(s"Error push message")
      //          throw new Exception(ex.toString())
      //        }
      //        case Success(pm) => {
      //          log.trace(s"Success!")
      //          pm.enqueuePushNotification(new SimpleApnsPushNotification(TokenUtil.tokenStringToByteArray(bogusToken), payload))
      //        }
      //      }

      tokens.par.foreach { token =>
        1 to 10 foreach { x =>
          println("firing APNS...")
          appleService.enqueuePushNotification(new SimpleApnsPushNotification(TokenUtil.tokenStringToByteArray(token), payload))
          Thread.sleep(3000)
        }
      }

      Thread.sleep(60000)
      1 must beEqualTo(1)
    }
  }
}
