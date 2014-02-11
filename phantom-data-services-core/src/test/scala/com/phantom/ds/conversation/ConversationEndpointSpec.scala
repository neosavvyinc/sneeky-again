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
import scala.concurrent.ExecutionContext.Implicits.global
import com.phantom.model.ConversationItem
import com.phantom.model.Conversation
import com.phantom.model.BlockUserByConversationResponse
import com.phantom.model.Contact

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

  "Conversation Service" should {

    "return a user's feed" in withSetupTeardown {
      insertTestConverationsWithItems()
      val toUserConv = conversationDao.insert(Conversation(None, 2L, 1L))
      val item = ConversationItem(None, toUserConv.id.get, "", "")
      await(conversationItemDao.insertAll(Seq(item, item, item)))
      val user = phantomUsersDao.find(2L)
      authedUser = user
      Get(s"/conversation") ~> conversationRoute ~> check {
        assertPayload[List[(Conversation, List[ConversationItem])]] { response =>

          response.foreach {
            case (conv, items) => {
              (conv.fromUser must be equalTo 2) or (conv.toUser must be equalTo 2)
              items must have size 3
            }
          }

          response must have size 2
        }
      }
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
          val items = conversationItemDao.findByConversationId(x.id.get)
          items should have size 1
          items.head.imageText must beEqualTo("This is the image text")
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
  }

  private def readImage : Array[Byte] = {
    val in4 = this.getClass.getClassLoader.getResourceAsStream("testFile.png")
    Iterator continually in4.read takeWhile (-1 !=) map (_.toByte) toArray
  }
}
