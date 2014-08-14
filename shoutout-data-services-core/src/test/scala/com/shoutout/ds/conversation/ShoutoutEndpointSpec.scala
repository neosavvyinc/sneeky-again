package com.shoutout.ds.conversation

import spray.http.StatusCodes._

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import com.shoutout.ds.framework.Logging
import com.shoutout.ds.PhantomEndpointSpec
import spray.http.{ BodyPart, MultipartFormData }
import com.shoutout.ds.framework.auth.SuppliedUserRequestAuthenticator
import akka.testkit.TestProbe
import akka.actor.ActorRef
import com.shoutout.model._
import com.shoutout.ds.dataAccess.BaseDAOSpec
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.shoutout.model.ConversationItem
import com.shoutout.model.Conversation
import com.shoutout.model.BlockUserByConversationResponse
import com.shoutout.model.Contact
import scala.slick.session.Session
import com.shoutout.ds.integration.amazon.S3Service
import com.shoutout.ds.integration.mock.TestS3Service

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 3:13 PM
 */
class ShoutoutEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with Logging
    with ShoutoutEndpoint
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
      Get("/shoutout/send") ~> shoutoutRoute ~> check {
        status == OK
      }
    }
  }
}
