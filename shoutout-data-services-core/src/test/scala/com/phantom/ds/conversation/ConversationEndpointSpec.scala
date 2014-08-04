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

    "return a simple hello message" in withSetupTeardown {
      Get("/conversation/hello") ~> conversationRoute ~> check {
        status == OK
      }
    }
  }
}
