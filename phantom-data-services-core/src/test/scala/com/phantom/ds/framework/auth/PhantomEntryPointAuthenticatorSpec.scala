package com.phantom.ds.framework.auth

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import org.joda.time.{ DateTimeZone, DateTime }
import java.security.MessageDigest
import java.net.URLEncoder
import org.apache.commons.codec.binary.Base64
import spray.routing.AuthenticationFailedRejection
import spray.http.StatusCodes._

class PhantomEntryPointAuthenticatorSpec extends Specification
    with AuthTestPoint
    with Specs2RouteTest {

  def actorRefFactory = system

  "PhantomEntryPointAuthenticator" should {
    "fail if no hash is detected" in {
      val d = now
      val url = s"/test/entry?$dateP=$d"
      assertAuthFailure(url)
    }

    "fail if no date is detected" in {
      val h = "bla"
      val url = s"/test/entry?$hashP=$h"
      assertAuthFailure(url)
    }

    "fail if date is not in ISO8601 format" in {
      val d = "bla"
      val h = hash(d)
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      assertAuthFailure(url)
    }

    "fail if hashes don't match" in {
      val d = now
      val h = hash(d, "badValue")
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      assertAuthFailure(url)
    }

    "fail if request timed out" in {
      val d = dateFormat.print(DateTime.parse("2010-10-10"))
      val h = hash(d)
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      assertAuthFailure(url)
    }

    "fail if hashed with wrong secret" in {
      val d = dateFormat.print(DateTime.parse("2010-10-10"))
      val h = hash(d, "wrongsecret")
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      assertAuthFailure(url)
    }

    "pass if all values are present and valid" in {
      val d = now
      val h = hash(d)
      val url = s"/test/entry?$hashP=$h&$dateP=$d"
      Post(url) ~> testRoute ~> check {
        status === OK
      }
    }
  }

  private def hash(date : String, secret : String = AuthConfiguration.secret) = {
    val digest = MessageDigest.getInstance("SHA-256")
    val concatBytes = s"${date}_$secret".getBytes("UTF-8")
    val hashedBytes = digest.digest(concatBytes)
    URLEncoder.encode(Base64.encodeBase64String(hashedBytes), "UTF-8")
  }

  private def now = dateFormat.print(DateTime.now(DateTimeZone.UTC))

  private def assertAuthFailure(url : String) = {
    Post(url) ~> testRoute ~> check {
      rejection must beAnInstanceOf[AuthenticationFailedRejection]
    }
  }

}