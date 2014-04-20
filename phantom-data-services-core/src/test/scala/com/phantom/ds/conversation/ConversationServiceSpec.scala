package com.phantom.ds.conversation

import org.specs2.mutable.{ After, Specification }
import akka.testkit.TestProbe
import com.phantom.ds.dataAccess.BaseDAOSpec
import com.phantom.ds.TestUtils
import akka.actor.ActorRefFactory
import spray.testkit.Specs2RouteTest
import com.phantom.model._
import com.phantom.ds.integration.mock.TestS3Service
import com.phantom.ds.integration.apple.AppleNotification
import com.phantom.model.FeedEntry
import com.phantom.ds.integration.twilio.SendInviteToStubUsers
import com.phantom.model.Contact

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

      val results = await(service.respondToConversation(starter.id.get, convo.id.get, "text", "my.imageurl.com"))
      aProbe.expectMsgAllOf(AppleNotification(true, Some("123456")))
      tProbe.expectNoMsg()

      results.id must beEqualTo(1)
    }

    "start a conversation normally if the receiver has mutualOnly set and the sender and receiver are connected" in withSetupTeardown {

      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

      val userA = createVerifiedUser("a@a.com", "123", "123")
      val userB = createVerifiedUser("b@b.com", "456", "456", true)
      val userC = createVerifiedUser("c@c.com", "789", "789")
      val bSession = await(sessions.createSession(PhantomSession.newSession(userB, Some("tokenB"))))
      val cSession = await(sessions.createSession(PhantomSession.newSession(userC, Some("tokenC"))))
      val aToB = Contact(None, userA.id.get, userB.id.get)
      val bToA = Contact(None, userB.id.get, userA.id.get)
      contacts.insertAll(Seq(aToB, bToA))

      val s = await(service.startConversation(userA.id.get, Set("456", "789"), "text", "url"))
      s.createdCount must beEqualTo(2)

      tProbe.expectNoMsg()
      aProbe.expectMsgAllOf(AppleNotification(true, bSession.pushNotifierToken), AppleNotification(true, cSession.pushNotifierToken))

      val feed = await(service.findFeed(userA.id.get, NoPaging))
      feed.foreach { feedEntry =>
        feedEntry.items must have size 1
        feedEntry.items.head.toUserDeleted must beEqualTo(false)
      }
      feed.size must beEqualTo(2)

      val bFeed = await(service.findFeed(userB.id.get, NoPaging))
      bFeed.foreach { feedEntry =>
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
      await(sessions.createSession(PhantomSession.newSession(userB, Some("tokenB"))))
      val cSession = await(sessions.createSession(PhantomSession.newSession(userC, Some("tokenC"))))

      val aToB = Contact(None, userA.id.get, userB.id.get)
      contacts.insertAll(Seq(aToB))

      val s = await(service.startConversation(userA.id.get, Set("456", "789"), "text", "url"))
      s.createdCount must beEqualTo(2)
      tProbe.expectNoMsg()
      aProbe.expectMsgAllOf(AppleNotification(true, cSession.pushNotifierToken))

      val expectedDeletionFlags = Map(userB.id.get -> true, userC.id.get -> false)

      val feed = await(service.findFeed(userA.id.get, NoPaging))
      feed.foreach { feedEntry =>
        feedEntry.items must have size 1
        val deleted = expectedDeletionFlags.get(feedEntry.conversation.toUser)
        feedEntry.items.head.toUserDeleted must beEqualTo(deleted.get)
      }
      feed.size must beEqualTo(2)

      val bFeed = await(service.findFeed(userB.id.get, NoPaging))
      bFeed should be empty

    }

    "starting a conversation with blocked users, or with users who block the user should not actually start a converation but appear as if it did" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)
      insertTestUsers()
      val contact2 = Contact(None, 2, 1)
      val contact3 = Contact(None, 3, 1)
      val contact4 = Contact(None, 4, 1, Blocked)
      val contact5 = Contact(None, 5, 1, Blocked)
      val contact6 = Contact(None, 1, 6, Blocked)
      contacts.insertAll(Seq(contact2, contact3, contact4, contact5, contact6))
      val s = await(service.startConversation(1, Set("222222", "333333", "444444", "555555", "666666"), "text", "url"))
      s.createdCount must beEqualTo(5)
      val expectedDeletionFlags = Map(2L -> false, 3L -> false, 4L -> true, 5L -> true, 6L -> false)
      val feed = await(service.findFeed(1L, NoPaging))
      feed.foreach { feedEntry =>
        feedEntry.items must have size 1
        val deleted = expectedDeletionFlags.get(feedEntry.conversation.toUser)
        feedEntry.items.head.toUserDeleted must beEqualTo(deleted.get)
      }
      feed.size must beEqualTo(5)

      val emptyFeedUsers = Seq(4L, 5L)
      val nonEmptyFeedUsers = Seq(2L, 3L, 6L)

      emptyFeedUsers.foreach { x =>
        val feed = await(service.findFeed(x, NoPaging))
        feed should beEmpty
      }

      nonEmptyFeedUsers.foreach { x =>
        val feed = await(service.findFeed(x, NoPaging))
        feed should have size 1
        feed.foreach { entry =>
          entry.items must have size 1
          entry.items.head.toUserDeleted must beFalse
        }
      }
      //to shut the compiler up
      1 should beEqualTo(1)
    }

    "responding to a conversation after recipient sets mutualOnly should not block any new conversation items if the users are connected" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)

      val userA = createVerifiedUser("a@a.com", "123", "123")
      val userB = createVerifiedUser("b@b.com", "456", "456", false)
      val bSession = await(sessions.createSession(PhantomSession.newSession(userB, Some("tokenB"))))

      val aToB = Contact(None, userA.id.get, userB.id.get)
      val bToA = Contact(None, userB.id.get, userA.id.get)
      contacts.insertAll(Seq(aToB, bToA))
      await(service.startConversation(userA.id.get, Set("456"), "text", "url"))
      val conversations = conversationDao.findByFromUserId(userA.id.get)
      phantomUsersDao.updateSetting(userB.id.get, MutualContactMessaging, true)
      await(service.respondToConversation(userA.id.get, conversations.head.id.get, "text", ".com"))

      tProbe.expectNoMsg()
      aProbe.expectMsgAllOf(AppleNotification(true, bSession.pushNotifierToken), AppleNotification(true, bSession.pushNotifierToken))

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
      val userB = createVerifiedUser("b@b.com", "456", "456", false)
      val bSession = await(sessions.createSession(PhantomSession.newSession(userB, Some("tokenB"))))
      val aToB = Contact(None, userA.id.get, userB.id.get)
      contacts.insertAll(Seq(aToB))
      await(service.startConversation(userA.id.get, Set("456"), "text", "url"))
      aProbe.expectMsg(AppleNotification(true, bSession.pushNotifierToken))
      val conversations = conversationDao.findByFromUserId(userA.id.get)
      phantomUsersDao.updateSetting(userB.id.get, MutualContactMessaging, true)
      Thread.sleep(1000)
      await(service.respondToConversation(userA.id.get, conversations.head.id.get, "text", ".com"))
      aProbe.expectNoMsg()
      tProbe.expectNoMsg()

      val aFeed = await(service.findFeed(userA.id.get, NoPaging))
      //REMEMBER: CONVERSATION ITEMS ARE IN ASCENDING ORDER..as OPPOSED TO CONVERSATIONS WHICH ARE IN DESCENDING ORDER.  Look at FeedFolder's 'toFeedEntry' function..thre is no sorting on the items list.
      aFeed.foreach { feedEntry =>
        feedEntry.items must have size 2
        val expectedDeleteFlags = Seq(false, true)
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

    "responding to a conversation after a user has blocked, should result in the recipient not seeing the post" in withSetupTeardown {
      val tProbe = TestProbe()
      val aProbe = TestProbe()
      val service = ConversationService(tProbe.ref, aProbe.ref, s3Service)
      insertTestUsers()
      await(service.startConversation(1L, Set("222222"), "text", "url"))
      contacts.insertAll(Seq(Contact(None, 2L, 1L, Blocked)))
      await(service.respondToConversation(1L, 1L, "bla", "bla"))
      val aFeed = await(service.findFeed(1L, NoPaging))
      aFeed.foreach { feedEntry =>
        feedEntry.items must have size 2
        val expectedDeleteFlags = Seq(false, true)
        val deletedFlags = feedEntry.items.map(_.toUserDeleted)
        deletedFlags must beEqualTo(expectedDeleteFlags)
      }

      val bFeed = await(service.findFeed(2L, NoPaging))
      bFeed.foreach { feedEntry =>
        feedEntry.items must have size 1
        val expectedDeleteFlags = Seq(false)
        val deletedFlags = feedEntry.items.map(_.toUserDeleted)
        deletedFlags must beEqualTo(expectedDeleteFlags)
      }

      aFeed should have size 1
      bFeed should have size 1
    }
  }

  override def after : Any = system.shutdown _
}
