package com.phantom.ds.conversation

import spray.http.StatusCodes._

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec
import scala.concurrent.future
import spray.http.{ BodyPart, MultipartFormData }
import com.phantom.ds.framework.auth.SuppliedUserRequestAuthenticator
import akka.testkit.TestProbe
import akka.actor.ActorRef
import com.phantom.model.{ ConversationItem, BlockUserByConversationResponse, Conversation }
import com.phantom.ds.dataAccess.BaseDAOSpec
import scala.concurrent.ExecutionContext.Implicits.global

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

    "support blocking a user by providing a conversation id" in withSetupTeardown {
      import scala.concurrent.duration._

      // using "executor" ExecutionContext from RouteTest trait
      val f = future {
        insertTestConverationsWithItems
        insertTestContacts
      }(executor)

      val waitForIt = scala.concurrent.Await.result(f, FiniteDuration(5, SECONDS))

      Post("/conversation/block/1") ~> conversationRoute ~> check {
        assertPayload[BlockUserByConversationResponse] { response =>
          response.id must be equalTo 1L
        }
      }
    }

    "return a user's feed" in withSetupTeardown {
      insertTestConverationsWithItems
      val toUserConv = conversationDao.insert(Conversation(None, 2L, 1L))
      val item = ConversationItem(None, toUserConv.id.get, "", "")
      await(conversationItemDao.insertAll(Seq(item, item, item)))
      val user = await(phantomUsersDao.find(2L))
      authedUser = Some(user) //yick
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

    "support receiving a multi-part form post to start a conversation with image" in withSetupTeardown {
      insertTestUsers

      val in4 = this.getClass().getClassLoader().getResourceAsStream("testFile.png")
      var stream = Iterator continually in4.read takeWhile (-1 !=) map (_.toByte) toArray

      val multipartFormWithData = MultipartFormData {
        Map(
          "imageText" -> BodyPart("This is the image text"),
          "userid" -> BodyPart("1"),
          "image" -> BodyPart(stream),
          "toUsers" -> BodyPart("111111,222222,333333")
        )
      }

      Post("/conversation/start", multipartFormWithData) ~> conversationRoute ~> check {
        status === OK
        val conversations = conversationDao.findByFromUserId(1)
        conversations.foreach { x =>
          val items = conversationItemDao.findByConversationId(x.id.get)
          items should have size 1
          items.head.imageText must beEqualTo("This is the image text")
        }
        conversations should have size 3
      }

    }

    "support receiving a multi-part form post to update a conversation with image" in withSetupTeardown {
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

    }

  }

}
