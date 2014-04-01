package com.phantom.ds.conversation

import spray.http.StatusCodes._

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec
import spray.http.{ BodyPart, MultipartFormData }
import com.phantom.ds.framework.auth.SuppliedUserRequestAuthenticator
import akka.testkit.TestProbe
import akka.actor.ActorRef
import com.phantom.model._
import com.phantom.ds.dataAccess.BaseDAOSpec
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.phantom.model.ConversationItem
import com.phantom.model.Conversation
import com.phantom.model.BlockUserByConversationResponse
import com.phantom.model.Contact
import scala.slick.session.Session
import com.phantom.ds.integration.amazon.S3Service
import com.phantom.ds.integration.mock.TestS3Service

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 3:13 PM
 */
class ConversationEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with Logging
    with ConversationEndpoint
    with SuppliedUserRequestAuthenticator
    with BaseDAOSpec {

  sequential

  def actorRefFactory = system

  val probe = TestProbe()
  val appleProbe = TestProbe()
  val twilioActor : ActorRef = probe.ref
  val appleActor : ActorRef = appleProbe.ref
  def s3Service : S3Service = new TestS3Service()

  "Conversation Service" should {

    implicit val routeTestTimeout = RouteTestTimeout(15 seconds span)

    "return a user's feed with default pagination parameters" in withSetupTeardown {
      insertTestConverationsWithItems()
      val toUserConv = conversationDao.insert(Conversation(None, 2L, 1L, "9197419597"))
      val item = ConversationItem(None, toUserConv.id.get, "", "", 2L, 1L)
      await(conversationItemDao.insertAll(Seq(item, item, item)))
      val user = phantomUsersDao.find(2L)
      authedUser = user
      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>

          response.foreach {
            case FeedWrapper(conv, items) => {
              items must have size 3
            }
          }

          response must have size 2
        }
      }
    }

    "return a user's feed with pagination parameters" in withSetupTeardown {
      insertTestConverationsWithItems()
      //make there be 40 conversations in the database with user 2 (the above insertion creates 1)
      db.withTransaction { implicit session : Session =>
        val c = for { i <- 1 to 39 } yield Conversation(None, 1, 2, "9197419597")
        val conversations = conversationDao.insertAllOperation(c)
        val ci = conversations.map(x => ConversationItem(None, x.id.get, "", "", 1L, 2L))
        conversationItemDao.insertAllOperation(ci)
      }

      authedUser = phantomUsersDao.find(2L)
      assertPagination(1, 20, 20) //page 1
      assertPagination(2, 20, 20) //page 2
      assertPagination(3, 20, 0) // past end of feed
      assertPagination(-1, -1, 40) //negatives are our "default turn off paging" for now
      assertPagination(1, 50, 40) //one massive page request should return everything
      assertPagination(1, 0, 0) //ask for 0, and ye shall receive 0
      assertPagination(3, 15, 10) //last page when the last page is smaller then the size
    }

    "support receiving a multi-part form post to start or update a conversation, if no image it throws error" in withSetupTeardown {

      insertTestUsers()
      authedUser = phantomUsersDao.find(2L)

      val multipartForm = MultipartFormData {
        Map(
          "imageText" -> BodyPart("This is the image text with no image")
        )
      }

      Post("/conversation/start", multipartForm) ~> conversationRoute ~> check {
        handled === false
      }

    }

    "support receiving a multi-part form post to start a conversation with image" in withSetupTeardown {
      insertTestUsers()
      authedUser = phantomUsersDao.find(2L)

      val multipartFormWithData = MultipartFormData {
        Map(
          "imageText" -> BodyPart("This is the image text"),
          "image" -> BodyPart(readImage),
          "toUsers" -> BodyPart("111111,222222,333333")
        )
      }

      Post("/conversation/start", multipartFormWithData) ~> conversationRoute ~> check {
        status === OK
        val conversations = conversationDao.findByFromUserId(2)
        conversations.foreach { x =>
          val items = db.withSession { implicit session : Session =>
            conversationItemDao.findByConversationIdAndUserOperation(x.id.get, authedUser.get.id.get)
          }
          items should have size 1
          items.head.imageText must beEqualTo("This is the image text")
        }
        conversations should have size 3
      }
    }

    "support starting a conversation with a stock photo" in withSetupTeardown {
      insertTestUsers()
      insertTestPhotoCategories()
      insertTestPhotos()
      authedUser = phantomUsersDao.find(2L)

      val req = ConversationStartRequest(Seq("111111", "222222", "333333"), "check this ooooout", 1)

      Post("/conversation/start/stock", req) ~> conversationRoute ~> check {
        status === OK
        val conversations = conversationDao.findByFromUserId(2)
        conversations.foreach { conversation =>
          val items = db.withSession { implicit session : Session =>
            conversationItemDao.findByConversationIdAndUserOperation(conversation.id.get, authedUser.get.id.get)
          }
          items should have size 1
          items.head.imageText must beEqualTo("check this ooooout")
          items.head.imageUrl must beEqualTo("/somewhere/1")
        }
        conversations should have size 3
      }
    }

    "support receiving a multi-part form post to update a conversation with image" in withSetupTeardown {
      insertTestConverationsWithItems()

      val multipartFormWithData = MultipartFormData {
        Map(
          "convId" -> BodyPart("1"),
          "imageText" -> BodyPart("This is the image text"),
          "image" -> BodyPart(readImage)
        )
      }

      Post("/conversation/respond", multipartFormWithData) ~> conversationRoute ~> check {
        status === OK
      }
    }

    "support responding to a conversation with a stock photo" in withSetupTeardown {
      insertTestPhotoCategories()
      insertTestPhotos()
      insertTestConverationsWithItems()
      authedUser = phantomUsersDao.find(2L)
      val req = ConversationRespondRequest(1, "check dat text", 1)

      Post("/conversation/respond/stock", req) ~> conversationRoute ~> check {
        status === OK
        val mostRecentConvo = conversationDao.findByFromUserId(2).last
        val items = db.withSession { implicit session : Session =>
          conversationItemDao.findByConversationIdAndUserOperation(mostRecentConvo.id.get, authedUser.get.id.get)
        }
        items.last.imageText must beEqualTo("check dat text")
        items.last.imageUrl must beEqualTo("/somewhere/1")
      }
    }

    "support setting viewed on a conversation item you are the toUser for" in withSetupTeardown {
      insertTestUsers()
      authedUser = phantomUsersDao.find(1L)
      insertTestConverationsWithItems()

      Post("/conversation/view/2") ~> conversationRoute ~> check {
        assertPayload[Boolean] { response =>
          status === OK and response === true
        }
      }

    }

    "support setting viewed on a conversation item you are not the toUser for" in withSetupTeardown {
      insertTestUsers()
      authedUser = phantomUsersDao.find(2L)
      insertTestConverationsWithItems()

      Post("/conversation/view/2") ~> conversationRoute ~> check {
        assertPayload[Boolean] { response =>
          status === OK and response === false
        }
      }

    }

    "disallow responding to a conversation if the user is not a member" in withSetupTeardown {
      insertTestConverationsWithItems()

      authedUser = phantomUsersDao.find(3L)

      val multipartFormWithData = MultipartFormData {
        Map(
          "convId" -> BodyPart("1"),
          "imageText" -> BodyPart("This is the image text"),
          "image" -> BodyPart(readImage)
        )
      }

      Post("/conversation/respond", multipartFormWithData) ~> conversationRoute ~> check {
        assertFailure(203)
      }
    }

    "support blocking a user by providing a conversation id for both from and to users" in withSetupTeardown {

      insertTestConverationsWithItems()
      insertTestContacts()

      await(contacts.insert(Contact(None, 2, 1)))

      val user1 = phantomUsersDao.find(1L)
      val user2 = phantomUsersDao.find(2L)

      authedUser = user1

      Post("/conversation/block/1") ~> conversationRoute ~> check {
        assertPayload[BlockUserByConversationResponse] { response =>
          response.id must be equalTo 1L
          val contact = await(contacts.findByContactId(1L, 2L))
          contact.contactType must be equalTo Blocked
        }
      }

      authedUser = user2

      Post("/conversation/block/1") ~> conversationRoute ~> check {
        assertPayload[BlockUserByConversationResponse] { response =>
          response.id must be equalTo 1L
          val contact = await(contacts.findByContactId(2L, 1L))
          contact.contactType must be equalTo Blocked
        }
      }
    }

    "fail blocking if the user is not a member of the conversation" in withSetupTeardown {
      insertTestConverationsWithItems()
      insertTestContacts()
      authedUser = phantomUsersDao.find(3L)
      Post("/conversation/block/1") ~> conversationRoute ~> check {
        assertFailure(203)
      }

    }

    "create blocked contact if the contact doesn't exist" in withSetupTeardown {
      insertTestConverationsWithItems()
      insertTestContacts()

      authedUser = phantomUsersDao.find(2L)

      Post("/conversation/block/1") ~> conversationRoute ~> check {
        assertPayload[BlockUserByConversationResponse] { response =>
          response.id must be equalTo 1L
          val contact = await(contacts.findByContactId(2L, 1L))
          contact.contactType must be equalTo Blocked
        }
      }
    }

    "fail blocking if the conversation doesn't exist" in withSetupTeardown {
      insertTestConverationsWithItems()
      insertTestContacts()
      authedUser = phantomUsersDao.find(3L)
      Post("/conversation/block/100") ~> conversationRoute ~> check {
        assertFailure(203)
      }
    }

    "deleting and item should show up as deleted in the deleter's feed, and still show up in the other person's feed" in withSetupTeardown {
      insertTestConverationsWithItems()
      val user1 = phantomUsersDao.find(1L)
      val user2 = phantomUsersDao.find(2L)
      authedUser = user1
      Delete("/conversation/deleteitem/1") ~> conversationRoute ~> check {
        status == OK
      }

      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>
          response must have size 1
          response.head.items must have size 2
          response.head.items.map(_.imageText) must not contain "imageText1"
        }
      }

      authedUser = user2
      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>
          response must have size 1
          response.head.items must have size 3
          response.head.items.map(_.imageText) must contain("imageText1")
        }
      }
    }

    "deleting an item in a conversation started by and sent to the same user should not show up in the feed" in withSetupTeardown {

      createVerifiedUser("aparrish@neosavvy.com", "password", "111111")
      val conv1 = new Conversation(None, 1, 1, "9197419597")
      conversationDao.insert(conv1)
      val conv1item1 = new ConversationItem(None, 1, "imageUrl1", "imageText1", 1, 1)
      val conv1item2 = new ConversationItem(None, 1, "imageUrl2", "imageText2", 1, 1)
      await(conversationItemDao.insertAll(Seq(conv1item1, conv1item2)))

      val user1 = phantomUsersDao.find(1L)
      authedUser = user1
      Delete("/conversation/deleteitem/1") ~> conversationRoute ~> check {
        status == OK
      }

      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>
          response must have size 1
          response.head.items must have size 1
          response.head.items.map(_.imageText) must contain("imageText2")
        }
      }

    }

    "fail if a user tries to delete an item that belongs to a conversation that they are not in" in withSetupTeardown {
      insertTestConverationsWithItems()
      val user1 = phantomUsersDao.find(1L)
      val user2 = phantomUsersDao.find(2L)
      val user3 = phantomUsersDao.find(3L)
      authedUser = user3
      Delete("/conversation/deleteitem/1") ~> conversationRoute ~> check {
        status == OK
      }

      authedUser = user1
      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>
          response must have size 1
          response.head.items must have size 3
          response.head.items.map(_.imageText) must contain("imageText1")
        }
      }

      authedUser = user2
      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>
          response must have size 1
          response.head.items must have size 3
          response.head.items.map(_.imageText) must contain("imageText1")
        }
      }
    }

    "deleting a conversation should mark all conversation items in a conversation as deleted, but not affect the other user" in withSetupTeardown {
      insertTestConverationsWithItems()
      val user1 = phantomUsersDao.find(1L)
      val user2 = phantomUsersDao.find(2L)
      authedUser = user1
      Delete("/conversation/delete/1") ~> conversationRoute ~> check {
        status == OK
      }

      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>
          response must have size 0
        }
      }

      authedUser = user2
      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>
          response must have size 1
          response.head.items must have size 3
        }
      }
    }

    "fail if a user tries to delete a conversation that they are not a member of" in withSetupTeardown {
      insertTestConverationsWithItems()
      val user1 = phantomUsersDao.find(1L)
      val user2 = phantomUsersDao.find(2L)
      val user3 = phantomUsersDao.find(3L)
      authedUser = user3
      Delete("/conversation/delete/1") ~> conversationRoute ~> check {
        status == OK
      }

      authedUser = user1
      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>
          response must have size 1
          response.head.items must have size 3
        }
      }

      authedUser = user2
      Get("/conversation") ~> conversationRoute ~> check {
        assertPayload[List[FeedWrapper]] { response =>
          response must have size 1
          response.head.items must have size 3
          response.head.items.map(_.imageText) must contain("imageText1")
        }
      }
    }

  }

  private def readImage : Array[Byte] = {
    val in4 = this.getClass.getClassLoader.getResourceAsStream("testFile.png")
    Iterator continually in4.read takeWhile (-1 !=) map (_.toByte) toArray
  }

  private def assertPagination(page : Int, size : Int, expected : Int) = {
    Get(s"/conversation?page=${page}&size=$size") ~> conversationRoute ~> check {
      assertPayload[List[FeedWrapper]] { response =>
        response must have size expected
      }
    }
  }
}
