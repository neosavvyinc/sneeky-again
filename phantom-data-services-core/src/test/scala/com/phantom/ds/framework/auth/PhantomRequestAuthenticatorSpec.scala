package com.phantom.ds.framework.auth

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import org.joda.time.{ DateTimeZone, DateTime }
import spray.http.StatusCodes._
import spray.routing.AuthenticationFailedRejection
import org.apache.commons.codec.binary.Base64
import java.net.URLEncoder
import java.security.MessageDigest

class PhantomRequestAuthenticatorSpec extends Specification
    with AuthTestPoint
    with Specs2RouteTest {

  def actorRefFactory = system

  "PhantomRequestAuthenticator" should {
    "fail if no hash is detected" in {
      val d = now
      val s = "noHash"
      val url = s"/test/protected?$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url)
    }

    "fail if no date is detected" in {
      val h = "bla"
      val s = "noDate"
      val url = s"/test/protected?$hashP=$h&$sessionIdP=$s"
      assertAuthFailure(url)
    }

    "fail if no sessionId is detected" in {
      val h = "bla"
      val d = now
      val url = s"/test/protected?$hashP=$h&$dateP=$d"
      assertAuthFailure(url)
    }

    "fail if date is not in ISO8601 format" in {
      val d = "bla"
      val s = "badDate"
      val h = hashValues(d, s)
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url)
    }

    "fail if hashes don't match" in {
      val d = now
      val s = "unmatchedHashes"
      val h = hashValues(d, "badValue")
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url)
    }

    "fail if request timed out" in {
      val d = dateFormat.print(DateTime.parse("2010-10-10"))
      val s = "timedOutRequest"
      val h = hashValues(d, s)
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url)
    }

    "fail if hashed with wrong secret" in {
      val d = dateFormat.print(DateTime.parse("2010-10-10"))
      val s = "timedOutRequest"
      val h = hashValues(d, s, "wrongsecret")
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url)
    }

    "pass if all values are present and valid" in {
      val d = now
      val s = "passingSession"
      val h = hashValues(d, s)
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      Get(url) ~> testRoute ~> check {
        status === OK
      }
    }
  }

  private def hashValues(date : String, sessionId : String, secret : String = AuthConfiguration.secret) = {
    val digest = MessageDigest.getInstance("SHA-256")
    val concatBytes = s"${date}_${sessionId}_$secret".getBytes("UTF-8")
    val hashedBytes = digest.digest(concatBytes)
    URLEncoder.encode(Base64.encodeBase64String(hashedBytes), "UTF-8")
  }

  private def now = dateFormat.print(DateTime.now(DateTimeZone.UTC))

  private def assertAuthFailure(url : String) = {
    Get(url) ~> testRoute ~> check {
      rejection must beAnInstanceOf[AuthenticationFailedRejection]
    }
  }

}