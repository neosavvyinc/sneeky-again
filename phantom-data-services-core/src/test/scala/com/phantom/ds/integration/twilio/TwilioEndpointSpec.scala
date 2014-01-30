package com.phantom.ds.integration.twilio

import org.specs2.mutable.Specification
import com.phantom.ds.PhantomEndpointSpec
import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import akka.actor.{ ActorRefFactory, ActorRef }
import akka.testkit.TestProbe
import spray.http.StatusCodes._

class TwilioEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with Logging
    with TwilioEndpoint {

  def actorRefFactory : ActorRefFactory = system

  val probe = TestProbe()
  val twilioActor : ActorRef = probe.ref //system.actorOf(Props(new TwilioActor(TwilioService(ec))))
  //probe.watch(twilioActor)

  sequential

  "Twilio Endpoint" should {
    "be able to receive invitation status callbacks" in {
      val registration = InviteMessageStatus("sid", "good")
      Post("/integration/invitation", registration) ~> twilioRoute ~> check {
        probe.expectMsg(registration)
        status mustEqual OK
      }
    }

  }

}