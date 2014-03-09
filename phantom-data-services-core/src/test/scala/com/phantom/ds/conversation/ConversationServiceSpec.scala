package com.phantom.ds.conversation

import org.specs2.mutable.{ After, Specification }
import akka.testkit.TestProbe
import com.phantom.ds.dataAccess.BaseDAOSpec
import com.phantom.ds.TestUtils
import akka.actor.ActorRefFactory
import spray.testkit.Specs2RouteTest
import com.phantom.ds.integration.twilio.SendInviteToStubUsers
import com.phantom.ds.integration.apple.AppleNotification
import com.phantom.model.{ MutualContactMessaging, Contact, NoPaging, FeedEntry }
import com.phantom.ds.integration.mock.TestS3Service

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

  val s3Service = new TestS3Service()

  "The Conversation Service" should {

    "start conversations with only phantom users" in withSetupTeardown {

      val tProbe = TestProbe()
      val aProbe = TestProbe()

      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

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
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

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
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

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
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

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
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

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
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

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

    //not testing probes here..as i'm being stubborn and until adam convinces me otherwise..i think there is a bug! :-p
    "start a conversation normally if the receiver has mutualOnly set and the sender and receiver are connected" in withSetupTeardown {

      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

      val userA = createVerifiedUser("a@a.com", "123", "123")
      val userB = createVerifiedUser("b@b.com", "456", "456", true)
      val userC = createVerifiedUser("c@c.com", "789", "780")
      val aToB = Contact(None, userA.id.get, userB.id.get)
      val bToA = Contact(None, userB.id.get, userA.id.get)
      contacts.insertAll(Seq(aToB, bToA))

      val s = await(service.startConversation(userA.id.get, Set("456", "789"), "text", "url"))
      s.createdCount must beEqualTo(2)

      val feed = await(service.findFeed(userA.id.get, NoPaging))
      feed.foreach { feedEntry =>
        feedEntry.items must have size 1
        feedEntry.items.head.toUserDeleted must beEqualTo(false)
      }
      feed.size must beEqualTo(2)

    }
    "start a conversation which appears deleted to the to user if the receiver has mutualOnly and they are not connected" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

      val userA = createVerifiedUser("a@a.com", "123", "123")
      val userB = createVerifiedUser("b@b.com", "456", "456", true)
      val userC = createVerifiedUser("c@c.com", "789", "789")

      val aToB = Contact(None, userA.id.get, userB.id.get)
      contacts.insertAll(Seq(aToB))

      val s = await(service.startConversation(userA.id.get, Set("456", "789"), "text", "url"))
      s.createdCount must beEqualTo(2)

      val expectedDeletionFlags = Map(userB.id.get -> true, userC.id.get -> false)

      val feed = await(service.findFeed(userA.id.get, NoPaging))
      feed.foreach { feedEntry =>
        feedEntry.items must have size 1
        val deleted = expectedDeletionFlags.get(feedEntry.conversation.toUser)
        feedEntry.items.head.toUserDeleted must beEqualTo(deleted.get)
      }
      feed.size must beEqualTo(2)
    }

    "responding to a conversation after recipient sets mutualOnly should not block any new conversation items if the users are connected" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

      val userA = createVerifiedUser("a@a.com", "123", "123")
      val userB = createVerifiedUser("b@b.com", "456", "456", true)
      val aToB = Contact(None, userA.id.get, userB.id.get)
      val bToA = Contact(None, userB.id.get, userA.id.get)
      contacts.insertAll(Seq(aToB, bToA))
      await(service.startConversation(userA.id.get, Set("456"), "text", "url"))
      val conversations = conversationDao.findByFromUserId(userA.id.get)
      phantomUsersDao.updateSetting(userB.id.get, MutualContactMessaging, true)
      await(service.respondToConversation(userA.id.get, conversations.head.id.get, "text", null))

      val aFeed = await(service.findFeed(userA.id.get, NoPaging))
      aFeed.foreach { feedEntry =>
        feedEntry.items must have size 2
        feedEntry.items.head.toUserDeleted must beEqualTo(false)
      }

      aFeed must have size 1

      val bFeed = await(service.findFeed(userB.id.get, NoPaging))
      bFeed.foreach { feedEntry =>
        feedEntry.items must have size 2
        feedEntry.items.head.toUserDeleted must beEqualTo(false)
      }

      bFeed must have size 1

    }

    "responding to a conversation after recipient sets mutualOnly should block any new conversation items to the recipient if the users are not connected" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

      val userA = createVerifiedUser("a@a.com", "123", "123")
      val userB = createVerifiedUser("b@b.com", "456", "456", true)
      val aToB = Contact(None, userA.id.get, userB.id.get)
      contacts.insertAll(Seq(aToB))
      await(service.startConversation(userA.id.get, Set("456"), "text", "url"))
      val conversations = conversationDao.findByFromUserId(userA.id.get)
      phantomUsersDao.updateSetting(userB.id.get, MutualContactMessaging, true)
      await(service.respondToConversation(userA.id.get, conversations.head.id.get, "text", null))

      val aFeed = await(service.findFeed(userA.id.get, NoPaging))
      aFeed.foreach { feedEntry =>
        feedEntry.items must have size 2
        val expectedDeleteFlags = Seq(true, false)
        val deletedFlags = feedEntry.items.map(_.toUserDeleted)
        deletedFlags must beEqualTo(expectedDeleteFlags)
      }

      aFeed must have size 1

      val bFeed = await(service.findFeed(userB.id.get, NoPaging))
      bFeed.foreach { feedEntry =>
        feedEntry.items must have size 1
        feedEntry.items.head.toUserDeleted must beEqualTo(false)
      }

      bFeed must have size 1
    }

  }

  override def after : Any = system.shutdown _
}
