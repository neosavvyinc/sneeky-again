package com.phantom.ds.conversation

import spray.http.StatusCodes._

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec
import spray.http.{ BodyPart, MultipartFormData }
import java.io.{ FileInputStream, FileOutputStream }

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 3:13 PM
 */
class ConversationEndpointSpec extends Specification with PhantomEndpointSpec with Specs2RouteTest with Logging with ConversationEndpoint {

  sequential

  def actorRefFactory = system

  "Conversation Service" should {
    "Should return a 102 NoFeedFoundException if there is no data for a user" in {
      Get("/conversation/1") ~> conversationRoute ~> check {
        assertFailure(102)
      }
    }

    "Should support receiving a multi-part form post to start or update a conversation, if no image it throws error" in {

      val multipartForm = MultipartFormData {
        Map(
          "imageText" -> BodyPart("This is the image text with no image"),
          "userid" -> BodyPart("adamparrish")
        )
      }

      Post("/conversation/startOrUpdate", multipartForm) ~> conversationRoute ~> check {
        handled === false
      }

    }

    "Should support receiving a multi-part form post to start or update a conversation with image" in {

      val source = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/testFile.png"))
      val byteArray = source.map(_.toByte).toArray
      source.close()

      val multipartFormWithData = MultipartFormData {
        Map(
          "imageText" -> BodyPart("This is the image text"),
          "userid" -> BodyPart("adamparrish"),
          "image" -> BodyPart(byteArray)
        )
      }

      Post("/conversation/startOrUpdate", multipartFormWithData) ~> conversationRoute ~> check {
        status === OK
      }

    }

    "Should support a simple image upload to prove that it works" in {

      val source = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/testFile.png"))
      val byteArray = source.map(_.toByte).toArray
      source.close()

      val multipartFormWithData = MultipartFormData {
        Map(
          "imageupload" -> BodyPart(byteArray)
        )
      }

      Post("/conversation/upload", multipartFormWithData) ~> conversationRoute ~> check {
        status === OK
      }

    }
  }

}
