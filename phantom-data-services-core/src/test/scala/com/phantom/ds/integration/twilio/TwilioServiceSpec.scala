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

  private val svc = TwilioService(global)

  sequential

  "Twilio Service" should {
    "be able to verify a registration" in withSetupTeardown {
      val user = createUnverifiedUser("email@email.com", "password")
      user.id must not beNone

      val regResponse = reg("pre", user.uuid.toString, "post")
      val d = FiniteDuration(5, SECONDS)
      Await.result(svc.verifyRegistration(regResponse), d)

      val updatedUser = Await.result(phantomUsers.find(user.id.get), d)
      updatedUser.status must be equalTo Verified
    }
  }

}