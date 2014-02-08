package com.phantom.ds.framework.auth

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import com.phantom.ds.dataAccess.BaseDAOSpec
import java.util.UUID
import com.phantom.model.PhantomSession
import org.joda.time.{ DateTimeZone, DateTime }
import spray.http.StatusCodes._

class NonHashingRequestAuthenticatorSpec extends Specification
    with AuthTestPoint
    with NonHashingRequestAuthenticator
    with PhantomEntryPointAuthenticator
    with Specs2RouteTest
    with AuthenticatedSpec
    with BaseDAOSpec {

  sequential

  def actorRefFactory = system

  "NonHashingRequestAuthenticator" should {

    "fail if no session parameter is passed" in withSetupTeardown {
      val url = s"/test/protected"
      assertAuthFailure(url, testRoute)
    }

    "fail if the session parameter doesn't resolve to a valid session" in withSetupTeardown {
      val uuid = UUID.randomUUID()
      val s = uuid.toString
      val url = s"/test/protected?$sessionIdP=$s"
      assertAuthFailure(url, testRoute)
    }

    "pass if the session is valid" in withSetupTeardown {
      val uuid = UUID.randomUUID()
      val s = uuid.toString
      val sessionCreated = DateTime.now(DateTimeZone.UTC)
      val url = s"/test/protected?$sessionIdP=$s"
      val user = createVerifiedUser("email@email.com", "blah")
      await(sessions.createSession(PhantomSession(uuid, user.id.get, sessionCreated, sessionCreated)))
      Get(url) ~> testRoute ~> check {
        status === OK
      }
    }

  }
}