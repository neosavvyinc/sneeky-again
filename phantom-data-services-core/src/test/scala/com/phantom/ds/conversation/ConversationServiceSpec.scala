package com.phantom.ds.conversation

import org.specs2.mutable.{ After, Specification }
import akka.testkit.TestProbe
import com.phantom.ds.dataAccess.BaseDAOSpec
import com.phantom.ds.TestUtils
import akka.actor.ActorRefFactory
import spray.testkit.Specs2RouteTest
import com.phantom.ds.integration.twilio.SendInviteToStubUsers
import com.phantom.ds.integration.apple.AppleNotification
import com.phantom.model.{ NoPaging, FeedEntry }

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/11/14
 * Time: 1:37 PM
 */
class ConversationServiceSpec extends Specification
    with BaseDAOSpec
    with Specs2RouteTest
    with TestUtils
    with After {

  def actorRefFactory : ActorRefFactory = system

  sequential

  "The Conversation Service" should {

    "start conversations with only phantom users" in withSetupTeardown {

      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref)

      val starter = createVerifiedUser("starter@starter.com", "password")
      val user1 = createVerifiedUser("email@email.com", "password", "12345")
      val user2 = createVerifiedUser("email2@email.com", "password", "56789")

      await {
        sessions.createSession(com.phantom.model.PhantomSession.newSession(user1, Some("123456")))
        sessions.createSession(com.phantom.model.PhantomSession.newSession(user2, Some("234567")))
      }

      val results = await(service.startConversation(starter.id.get, Set("12345", "56789"), "text", "url"))

      results.createdCount must beEqualTo(2)

      val userIds = Seq(user1.id, user2.id).flatten
      val user1Conversation = await(service.findFeed(starter.id.get, NoPaging))

      aProbe.expectMsgAllOf(AppleNotification(true, Some("123456")), AppleNotification(true, Some("234567")))
      tProbe.expectNoMsg()

      user1Conversation.foreach {
        case FeedEntry(c, items) =>
          items must have size 1
          items.head.imageText must beEqualTo("text")
          items.head.imageUrl must beEqualTo("url")
          c.toUser must beOneOf(userIds : _*)
      }
      user1Conversation must have size 2
    }

    "start conversations with only stub users" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref)

      val stubUser1 = createStubUser("123")
      val stubUser2 = createStubUser("456")
      val stubUsers = Seq(stubUser1, stubUser2)
      val starter = createVerifiedUser("starter@starter.com", "password")
      val results = await(service.startConversation(starter.id.get, Set("123", "456"), "text", "url"))

      results.createdCount must beEqualTo(2)

      val userIds = stubUsers.map(_.id.get)

      val startedStubs = await(service.findFeed(starter.id.get, NoPaging))

      tProbe.expectMsg(SendInviteToStubUsers(stubUsers))
      aProbe.expectNoMsg()

      startedStubs.foreach {
        case FeedEntry(c, items) =>
          c.toUser must beOneOf(userIds : _*)
          items must have size 1
          items.head
          items.head.imageText must beEqualTo("text")
          items.head.imageUrl must beEqualTo("url")
      }

      startedStubs must have size 2

    }

    "start conversations with only unidentified users " in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref)

      val starter = createVerifiedUser("starter@starter.com", "password")
      val results = await(service.startConversation(starter.id.get, Set("123", "456"), "text", "url"))

      val newUsers = await(phantomUsersDao.findByPhoneNumbers(Set("123", "456")))

      newUsers must have size 2

      tProbe.expectMsg(SendInviteToStubUsers(newUsers))
      aProbe.expectNoMsg()

      results.createdCount must beEqualTo(2)
    }

    "start conversations with a mix of all three types of users" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref)

      val starter = createVerifiedUser("starter@starter.com", "password")
      val user1 = createVerifiedUser("email@email.com", "password", "12")
      val user2 = createVerifiedUser("email2@email.com", "password", "34")

      await {
        sessions.createSession(com.phantom.model.PhantomSession.newSession(user1, Some("3456789")))
        sessions.createSession(com.phantom.model.PhantomSession.newSession(user2, Some("4567890")))
      }

      val stubUser1 = createStubUser("56")
      val stubUser2 = createStubUser("78")
      val stubUsers = Seq(stubUser1, stubUser2)

      val nums = Set("12", "34", "56", "78", "90", "09")
      val results = await(service.startConversation(starter.id.get, nums, "text", "url"))

      val newUsers = await(phantomUsersDao.findByPhoneNumbers(Set("90", "09")))

      val newUserIds = newUsers.map(_.id).flatten

      val user1Conversation = await(service.findFeed(starter.id.get, NoPaging))
      val userIds = Seq(user1.id, user2.id, stubUser1.id, stubUser2.id).flatten ++ newUserIds

      user1Conversation.foreach {
        case FeedEntry(c, items) =>
          items must have size 1
          items.head.imageText must beEqualTo("text")
          items.head.imageUrl must beEqualTo("url")
          c.toUser must beOneOf(userIds : _*)
      }

      aProbe.expectMsgAllOf(AppleNotification(true, Some("3456789")), AppleNotification(true, Some("4567890")))
      tProbe.expectMsg(SendInviteToStubUsers(stubUsers ++ newUsers))
      results.createdCount must beEqualTo(6)
    }

    "not send invitations to stub users if their invitation count is maxed out" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref)

      createStubUser("888", 3)
      createStubUser("999", 3)
      val starter = createVerifiedUser("starter@starter.com", "password")
      val results = await(service.startConversation(starter.id.get, Set("888", "999"), "text", "url"))

      aProbe.expectNoMsg()
      tProbe.expectNoMsg()
      results.createdCount must beEqualTo(2)
    }

    "send APNS notifications when responding to a conversation" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref)

      val starter = createVerifiedUser("starter@starter.com", "password")
      val receiver = createVerifiedUser("email@email.com", "password", "12345")

      await {
        sessions.createSession(com.phantom.model.PhantomSession.newSession(receiver, Some("123456")))
      }

      val convo = createConversation(starter.id.get, receiver.id.get)

      val results = await(service.respondToConversation(starter.id.get, convo.id.get, "text", new Array[Byte](1)))
      aProbe.expectMsgAllOf(AppleNotification(true, Some("123456")))
      tProbe.expectNoMsg()

      results.id must beEqualTo(1)
    }

  }

  override def after : Any = system.shutdown _
}
