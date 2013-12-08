package com.phantom.ds.conversation

import spray.http.StatusCodes._

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 3:13 PM
 */
class ConversationEndpointSpec extends Specification with PhantomEndpointSpec with Specs2RouteTest with Logging with ConversationEndpoint {

  def actorRefFactory = system

  "Conversation Service" should {
    "Should return a 102 NoFeedFoundException if there is no data for a user" in {
      Get("/conversation/1") ~> conversationRoute ~> check {
        assertFailure(102)
      }
    }
  }

}
