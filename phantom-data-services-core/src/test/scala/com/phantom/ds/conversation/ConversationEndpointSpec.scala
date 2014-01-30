package com.phantom.ds.conversation

import spray.http.StatusCodes._

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec
import scala.concurrent.{ Promise, Future, future }
import spray.http.{ BodyPart, MultipartFormData }
import java.io.{ FileInputStream, FileOutputStream }
import com.phantom.model.{ ConversationItem, PhantomUser, BlockUserByConversationResponse, Conversation }
import java.util.UUID
import org.joda.time.LocalDate
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.dataAccess.BaseDAOSpec

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
    with DatabaseSupport
    with BaseDAOSpec {

  sequential

  def actorRefFactory = system

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

    "return just one conversation with all 1's" in withSetupTeardown {
      insertTestConverationsWithItems

      Get("/conversation/1") ~> conversationRoute ~> check {
        assertPayload[List[(Conversation, List[ConversationItem])]] { response =>
          response.length must be equalTo (2)
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
          "toUsers" -> BodyPart("1,2,3")
        )
      }

      Post("/conversation/start", multipartFormWithData) ~> conversationRoute ~> check {
        status === OK
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
