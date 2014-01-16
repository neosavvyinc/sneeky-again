package com.phantom.ds.conversation

import spray.http.StatusCodes._

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec
import spray.http.{ BodyPart, MultipartFormData }
import java.io.{ FileInputStream, FileOutputStream }
import com.phantom.model.{ ConversationItem, PhantomUser, BlockUserByConversationResponse, Conversation }
import java.util.UUID
import org.joda.time.LocalDate
import com.phantom.dataAccess.DatabaseSupport

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 3:13 PM
 */
class ConversationEndpointSpec extends Specification with PhantomEndpointSpec with Specs2RouteTest with Logging with ConversationEndpoint with DatabaseSupport {

  sequential

  def insertTestUsers {
    val user1 = new PhantomUser(None, UUID.randomUUID(), "aparrish@neosavvy.com", "password", new LocalDate(1981, 8, 10), true, "1234567")
    val user2 = new PhantomUser(None, UUID.randomUUID(), "ccaplinger@neosavvy.com", "password", new LocalDate(1986, 10, 12), true, "1234567")
    val user3 = new PhantomUser(None, UUID.randomUUID(), "tewen@neosavvy.com", "password", new LocalDate(1987, 8, 16), true, "1234567")
    val user4 = new PhantomUser(None, UUID.randomUUID(), "dhamlettneosavvy.com", "password", new LocalDate(1985, 5, 17), true, "1234567")
    val user5 = new PhantomUser(None, UUID.randomUUID(), "nick.sauro@gmail.com", "password", new LocalDate(1987, 8, 16), true, "1234567")
    val user6 = new PhantomUser(None, UUID.randomUUID(), "pablo.alonso@gmail.com", "password", new LocalDate(1987, 8, 16), true, "1234567")
    phantomUsers.insert(user1)
    phantomUsers.insert(user2)
    phantomUsers.insert(user3)
    phantomUsers.insert(user4)
    phantomUsers.insert(user5)
    phantomUsers.insert(user6)
  }

  def insertTestConversations {

    val conv1 = new Conversation(None, 1, 2)
    val conv2 = new Conversation(None, 3, 4)
    val conv3 = new Conversation(None, 5, 6)
    conversations.insert(conv1)
    conversations.insert(conv2)
    conversations.insert(conv3)

  }

  def insertTestUsersAndConversations {
    insertTestUsers
    insertTestConversations
  }

  def insertTestConverationsWithItems {
    insertTestUsersAndConversations

    val conv1item1 = new ConversationItem(None, 1, "imageUrl1", "imageText1")
    val conv1item2 = new ConversationItem(None, 1, "imageUrl2", "imageText2")
    val conv1item3 = new ConversationItem(None, 1, "imageUrl3", "imageText3")

    val conv2item1 = new ConversationItem(None, 2, "imageUrl1", "imageText1")
    val conv2item2 = new ConversationItem(None, 2, "imageUrl2", "imageText2")
    val conv2item3 = new ConversationItem(None, 2, "imageUrl3", "imageText3")

    conversationItems.insert(conv1item1)
    conversationItems.insert(conv1item2)
    conversationItems.insert(conv1item3)
    conversationItems.insert(conv2item1)
    conversationItems.insert(conv2item2)
    conversationItems.insert(conv2item3)
  }

  def actorRefFactory = system

  "Conversation Service" should {
    "return just one conversation with all 1's" in {
      Get("/conversation/1") ~> conversationRoute ~> check {
        assertPayload[List[Conversation]] { response =>
          response(0).id must be equalTo Some(1)
          response(0).fromUser must be equalTo 1
          response(0).toUser must be equalTo 1
        }
      }
    }

    "support receiving a multi-part form post to start or update a conversation, if no image it throws error" in {

      val multipartForm = MultipartFormData {
        Map(
          "imageText" -> BodyPart("This is the image text with no image"),
          "userid" -> BodyPart("adamparrish")
        )
      }

      Post("/conversation/start", multipartForm) ~> conversationRoute ~> check {
        handled === false
      }

    }

    "support receiving a multi-part form post to start a conversation with image" in {
      insertTestUsers

      val in4 = this.getClass().getClassLoader().getResourceAsStream("testFile.png")
      var stream = Iterator continually in4.read takeWhile (-1 !=) map (_.toByte) toArray

      val multipartFormWithData = MultipartFormData {
        Map(
          "imageText" -> BodyPart("This is the image text"),
          "userid" -> BodyPart("1"),
          "image" -> BodyPart(stream),
          "toUsers" -> BodyPart("1,2,3")
        )
      }

      Post("/conversation/start", multipartFormWithData) ~> conversationRoute ~> check {
        status === OK
      }

    }

    "support receiving a multi-part form post to update a conversation with image" in {
      insertTestConverationsWithItems

      val in4 = this.getClass().getClassLoader().getResourceAsStream("testFile.png")
      var stream = Iterator continually in4.read takeWhile (-1 !=) map (_.toByte) toArray

      val multipartFormWithData = MultipartFormData {
        Map(
          "convId" -> BodyPart("1"),
          "imageText" -> BodyPart("This is the image text"),
          "image" -> BodyPart(stream)
        )
      }

      Post("/conversation/respond", multipartFormWithData) ~> conversationRoute ~> check {
        status === OK
      }

    }.pendingUntilFixed("Adam will fix this")

    "support blocking a user by providing a conversation id" in {

      Post("/conversation/block/1") ~> conversationRoute ~> check {
        assertPayload[BlockUserByConversationResponse] { response =>
          response.id must be equalTo 1L
        }
      }

    }.pendingUntilFixed("This is unimplemented")
  }

}
