package com.phantom.ds.framework.auth

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import org.joda.time.{ LocalDate, DateTimeZone, DateTime }
import spray.http.StatusCodes._
import com.phantom.ds.dataAccess.BaseDAOSpec
import java.util.UUID
import com.phantom.model._
import com.phantom.model.ShoutoutUser
import com.phantom.ds.framework.Dates

class PhantomRequestAuthenticatorSpec extends Specification
    with PhantomRequestAuthenticator
    with PhantomEntryPointAuthenticator
    with AuthTestPoint
    with Specs2RouteTest
    with BaseDAOSpec
    with AuthenticatedSpec {

  sequential

  def actorRefFactory = system

  //  "PhantomRequestAuthenticator" should {
  //    "fail if no hash is detected" in {
  //      val d = now
  //      val s = "noHash"
  //      val url = s"/test/protected?$dateP=$d&$sessionIdP=$s"
  //      assertAuthFailure(url, testRoute)
  //    }
  //
  //    "fail if no date is detected" in {
  //      val h = "bla"
  //      val s = "noDate"
  //      val url = s"/test/protected?$hashP=$h&$sessionIdP=$s"
  //      assertAuthFailure(url, testRoute)
  //    }
  //
  //    "fail if no sessionId is detected" in {
  //      val h = "bla"
  //      val d = now
  //      val url = s"/test/protected?$hashP=$h&$dateP=$d"
  //      assertAuthFailure(url, testRoute)
  //    }
  //
  //    "fail if date is not in ISO8601 format" in {
  //      val d = "bla"
  //      val s = "badDate"
  //      val h = hashValues(d, s)
  //      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
  //      assertAuthFailure(url, testRoute)
  //    }
  //
  //    "fail if hashes don't match" in {
  //      val d = now
  //      val s = "unmatchedHashes"
  //      val h = hashValues(d, "badValue")
  //      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
  //      assertAuthFailure(url, testRoute)
  //    }
  //
  //    "fail if request timed out" in {
  //      val d = Dates.write(DateTime.parse("2010-10-10"))
  //      val s = "timedOutRequest"
  //      val h = hashValues(d, s)
  //      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
  //      assertAuthFailure(url, testRoute)
  //    }
  //
  //    "fail if hashed with wrong secret" in {
  //      val d = Dates.write(DateTime.parse("2010-10-10"))
  //      val s = "timedOutRequest"
  //      val h = hashValues(d, s, "wrongsecret")
  //      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
  //      assertAuthFailure(url, testRoute)
  //    }
  //
  //    "fail if all values are present and valid but there is no session" in withSetupTeardown {
  //      val d = now
  //      val s = UUID.randomUUID().toString
  //      val h = hashValues(d, s)
  //      val url = s"/test/protected?$hashP=$h&$dateP=$d&$sessionIdP=$s"
  //      assertAuthFailure(url, testRoute)
  //    }
  //  }
  //
  //  "Authorization executing in a hierarchical fashion" should {
  //    "pass if a verified user accesses a verified protected endpoint" in withSetupTeardown {
  //      assertAuthorizationSuccess(Verified, "protected")
  //    }
  //
  //    "fail if an unverified user accesses a verifed protected endpoint" in withSetupTeardown {
  //      assertAuthorizationFailure(Unverified, "protected")
  //    }
  //
  //    "fail if a stub user accesses a verified protected endpoint" in withSetupTeardown {
  //      assertAuthorizationFailure(Stub, "protected")
  //    }
  //
  //    "pass if all values are present and valid and there is a session for unverified endpoint and verified user" in withSetupTeardown {
  //      assertAuthorizationSuccess(Verified, "unverified")
  //    }
  //
  //    "pass if all values are present and valid and there is a session for unverified endpoint and unverified user" in withSetupTeardown {
  //      assertAuthorizationSuccess(Unverified, "unverified")
  //    }
  //
  //    "fail if a stub user accesses an unverified protected endpoint" in withSetupTeardown {
  //      assertAuthorizationFailure(Stub, "unverified")
  //    }
  //  }
  //
  //  private def assertAuthorizationSuccess(userStatus : UserStatus, urlPart : String) = {
  //    val d = now
  //    val sessionCreated = Dates.nowDT
  //    val uuid = UUID.randomUUID()
  //    val s = uuid.toString
  //    val u = phantomUsersDao.insert(PhantomUser(None, UUID.randomUUID, Some("email"), Some(""), Some(Dates.nowLD), true, Some(""), userStatus))
  //    await(sessions.createSession(PhantomSession(uuid, u.id.get, sessionCreated, sessionCreated, None)))
  //    val h = hashValues(d, s)
  //    val url = s"/test/$urlPart?$hashP=$h&$dateP=$d&$sessionIdP=$s"
  //    Get(url) ~> testRoute ~> check {
  //      status === OK
  //    }
  //  }
  //
  //  private def assertAuthorizationFailure(userStatus : UserStatus, urlPart : String) = {
  //    val d = now
  //    val sessionCreated = Dates.nowDT
  //    val uuid = UUID.randomUUID()
  //    val s = uuid.toString
  //    val u = phantomUsersDao.insert(PhantomUser(None, UUID.randomUUID, Some("email"), Some(""), Some(Dates.nowLD), true, Some(""), userStatus))
  //    await(sessions.createSession(PhantomSession(uuid, u.id.get, sessionCreated, sessionCreated, None)))
  //    val h = hashValues(d, s)
  //    val url = s"/test/$urlPart?$hashP=$h&$dateP=$d&$sessionIdP=$s"
  //    assertAuthFailure(url, testRoute)
  //  }
}