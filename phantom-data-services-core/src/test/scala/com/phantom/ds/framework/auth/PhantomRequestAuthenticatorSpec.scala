package com.phantom.ds.framework.auth

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import org.joda.time.{ LocalDate, DateTimeZone, DateTime }
import spray.http.StatusCodes._
import com.phantom.ds.dataAccess.BaseDAOSpec
import java.util.UUID
import com.phantom.model.{ Verified, Unverified, PhantomUser, PhantomSession }
import scala.concurrent._
import scala.concurrent.duration._

class PhantomRequestAuthenticatorSpec extends Specification
    with PhantomRequestAuthenticator
    with PhantomEntryPointAuthenticator
    with AuthTestPoint
    with Specs2RouteTest
    with BaseDAOSpec
    with AuthenticatedSpec {

  sequential

  def actorRefFactory = system

  "PhantomRequestAuthenticator" should {
    "fail if no hash is detected" in {
      val d = now
      val s = "noHash"
      val url = s"/test/protected?$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url, testRoute)
    }

    "fail if no date is detected" in {
      val h = "bla"
      val s = "noDate"
      val url = s"/test/protected?$hashP=$h&$sessionIdP=$s"
      assertAuthFailure(url, testRoute)
    }

    "fail if no sessionId is detected" in {
      val h = "bla"
      val d = now
      val url = s"/test/protected?$hashP=$h&$dateP=$d"
      assertAuthFailure(url, testRoute)
    }

    "fail if date is not in ISO8601 format" in {
      val d = "bla"
      val s = "badDate"
      val h = hashValues(d, s)
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url, testRoute)
    }

    "fail if hashes don't match" in {
      val d = now
      val s = "unmatchedHashes"
      val h = hashValues(d, "badValue")
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url, testRoute)
    }

    "fail if request timed out" in {
      val d = dateFormat.print(DateTime.parse("2010-10-10"))
      val s = "timedOutRequest"
      val h = hashValues(d, s)
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url, testRoute)
    }

    "fail if hashed with wrong secret" in {
      val d = dateFormat.print(DateTime.parse("2010-10-10"))
      val s = "timedOutRequest"
      val h = hashValues(d, s, "wrongsecret")
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url, testRoute)
    }

    "fail if all values are present and valid but there is no session" in withSetupTeardown {
      val d = now
      val s = UUID.randomUUID().toString
      val h = hashValues(d, s)
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url, testRoute)
    }

    "pass if all values are present and valid and there is a session" in withSetupTeardown {

      val waitPeriod = Duration(1000, MILLISECONDS)
      val d = now
      val sessionCreated = DateTime.now(DateTimeZone.UTC)
      val uuid = UUID.randomUUID()
      val s = uuid.toString
      val u = phantomUsersDao.insert(PhantomUser(None, UUID.randomUUID, Some("email"), Some(""), Some(LocalDate.now), true, Some(""), false, false, 2, Verified))
      Await.result(sessions.createSession(PhantomSession(uuid, u.id.get, sessionCreated, sessionCreated, None)), waitPeriod)
      val h = hashValues(d, s)
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      Get(url) ~> testRoute ~> check {
        status === OK
      }
    }

    "fail if all values are present and valid and there is a session but the user is unverified" in withSetupTeardown {
      val waitPeriod = Duration(1000, MILLISECONDS)
      val d = now
      val sessionCreated = DateTime.now(DateTimeZone.UTC)
      val uuid = UUID.randomUUID()
      val s = uuid.toString
      val u = phantomUsersDao.insert(PhantomUser(None, UUID.randomUUID, Some("email"), Some(""), Some(LocalDate.now), true, Some(""), false, false, 1, Unverified))
      Await.result(sessions.createSession(PhantomSession(uuid, u.id.get, sessionCreated, sessionCreated, None)), waitPeriod)
      val h = hashValues(d, s)
      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      assertAuthFailure(url, testRoute)
    }
  }
}