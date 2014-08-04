package com.phantom.ds.integration.twilio

import org.specs2.mutable.Specification
import com.phantom.ds.dataAccess.BaseDAOSpec
import com.phantom.ds.TestUtils
import scala.concurrent.Future
import org.specs2.mock.Mockito
import com.twilio.sdk.resource.instance.Sms
import scala.concurrent.ExecutionContext.Implicits.global

class TwilioServiceSpec extends Specification
    with BaseDAOSpec
    with TestUtils
    with Mockito {

  sequential

  "The Twilio Service" should {

    "update stub users invitation count for successfully sent invitations to stub users" in withSetupTeardown {
      val stubUser = createStubUser("123")
      val sender = mock[TwilioMessageSender]
      sender.sendInvitations(any[Seq[String]]) returns Future.successful(Seq(Right(new Sms(null)), Right(new Sms(null))))
      val svc = TwilioService(sender)
      val results = await(svc.sendInvitationsToStubUsers(Seq(stubUser)))

      results must beEmpty
      val updatedStubUsers = await(phantomUsersDao.findByPhoneNumbers(Set("123")))

      updatedStubUsers.foreach { x =>
        x.invitationCount must beEqualTo(2)
      }

      updatedStubUsers must have size 1
    }

  }

}