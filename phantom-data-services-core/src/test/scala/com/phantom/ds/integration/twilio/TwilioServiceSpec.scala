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

    //combine these 3 tests into one..they are totally identical and hit the same code
    "create stub users and conversations for successfully sent invitations to unidentified contacts" in withSetupTeardown {

      val contacts = Set("123", "456")
      val sender = mock[TwilioMessageSender]
      sender.sendInvitations(any[Seq[String]]) returns Future.successful(Seq(Right(new Sms(null)), Right(new Sms(null))))
      val svc = TwilioService(sender)
      val results = await(svc.sendInvitationsToUnidentifiedUsers(SendInvite(contacts, 1, "text", "url")))

      results must beEmpty

      val stubUsers = await(phantomUsersDao.findByPhoneNumbers(contacts))
      val stubIds = stubUsers.map(_.id.get)
      val stubConversations = await(conversationDao.findConversationsAndItems(1))

      stubUsers must have size 2
      stubConversations.foreach {
        case (c, items) =>
          c.toUser must beOneOf(stubIds : _*)
          items must have size 1
          items.head.imageText must be equalTo "text"
          items.head.imageUrl must be equalTo "url"
      }

      stubConversations must have size 2
    }

    "not create stub users for any unidentified contacts that twilio rejects" in withSetupTeardown {
      val contacts = Set("123", "456")
      val sender = mock[TwilioMessageSender]
      sender.sendInvitations(any[Seq[String]]) returns Future.successful(Seq(Left(InvalidNumber), Left(InvalidNumber)))
      val svc = TwilioService(sender)
      val results = await(svc.sendInvitationsToUnidentifiedUsers(SendInvite(contacts, 1, "text", "url")))

      results must beEmpty

      val stubUsers = await(phantomUsersDao.findByPhoneNumbers(contacts))
      stubUsers must beEmpty
      val stubConversations = await(conversationDao.findConversationsAndItems(1))
      stubConversations must beEmpty
    }

    //NOT IMPORTANT FOR NOW..NOT RETRYING
    /*"return a list of unidentified contacts whose invitations could not be sent" in withSetupTeardown {
      val contacts = Set("123", "456")
      val sender = mock[TwilioMessageSender]
      sender.sendInvitations(any[Seq[String]]) returns Future.successful(Seq(Left(NonTwilioException(new Exception("test"))), Left(NonTwilioException(new Exception("test")))))
      val svc = TwilioService(sender)
      val results = await(svc.sendInvitationsToUnidentifiedUsers(SendInvite(contacts, 1, "text", "url")))

      results must have size 2

      val stubUsers = await(stubUsersDao.findByPhoneNumbers(contacts))
      stubUsers must beEmpty
      val stubConversations = await(stubConversationsDao.findByFromUserId(1))
      stubConversations must beEmpty
    }*/

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

    //NOT IMPORTANT FOR NOW..NOT RETRYING
    /*"return a list of stub users whose invitations could not be sent" in withSetupTeardown {
      1 must beEqualTo(1)
    }*/

  }

}