package com.shoutout.ds.framework.auth

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import org.joda.time.DateTime
import spray.routing._
import spray.http.StatusCodes._
import com.shoutout.ds.framework.Dates

class SessionDateHashEntryPointAuthenticatorSpec extends Specification
    with AuthTestPoint
    with SessionDateHashRequestAuthenticator
    with PhantomEntryPointAuthenticator
    with Specs2RouteTest
    with AuthenticatedSpec {

  sequential

  def actorRefFactory = system

  "PhantomEntryPointAuthenticator" should {
    "fail if no hash is detected" in {
      val d = now
      val url = s"/test/entry?$dateP=$d"
      assertPostAuthFailure(url)
    }

    "fail if no date is detected" in {
      val h = "bla"
      val url = s"/test/entry?$hashP=$h"
      assertPostAuthFailure(url)
    }

    "fail if date is not in ISO8601 format" in {
      val d = "bla"
      val h = hashEntryValues(d)
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      assertPostAuthFailure(url)
    }

    "fail if hashes don't match" in {
      val d = now
      val h = hashEntryValues(d, "badValue")
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      assertPostAuthFailure(url)
    }

    "fail if request timed out" in {
      val d = Dates.write(DateTime.parse("2010-10-10"))
      val h = hashEntryValues(d)
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      assertPostAuthFailure(url)
    }

    "fail if hashed with wrong secret" in {
      val d = Dates.write(DateTime.parse("2010-10-10"))
      val h = hashEntryValues(d, "wrongsecret")
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      assertPostAuthFailure(url)
    }

    "pass if all values are present and valid" in {
      val d = now
      val h = hashEntryValues(d)
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      Post(url) ~> testRoute ~> check {
        status === OK
      }
    }
  }
  protected def assertPostAuthFailure(url : String) = {
    Post(url) ~> testRoute ~> check {
      rejection must beAnInstanceOf[AuthenticationFailedRejection]
    }
  }

}