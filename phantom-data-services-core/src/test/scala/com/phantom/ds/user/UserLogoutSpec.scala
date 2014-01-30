package com.phantom.ds.user

import org.specs2.mutable.Specification
import com.phantom.ds.PhantomEndpointSpec
import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.auth.{ AuthenticatedSpec, PhantomEntryPointAuthenticator, PassThroughEntryPointAuthenticator, PhantomRequestAuthenticator }
import com.phantom.ds.dataAccess.BaseDAOSpec
import scala.concurrent.duration._
import org.joda.time.{ LocalDate, DateTimeZone, DateTime }
import java.util.UUID
import com.phantom.model.{ PhantomSession, Verified, PhantomUser }
import scala.concurrent.Await
import spray.http.StatusCodes._
import spray.routing.AuthenticationFailedRejection

class UserLogoutSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with PhantomRequestAuthenticator
    with PhantomEntryPointAuthenticator
    with UserEndpoint
    with AuthenticatedSpec
    with BaseDAOSpec {

  def actorRefFactory = system

  sequential

  "Logout" should {

    "work" in withSetupTeardown {
      val waitPeriod = Duration(1000, MILLISECONDS)
      val d = now
      val sessionCreated = DateTime.now(DateTimeZone.UTC)
      val uuid = UUID.randomUUID()
      val s = uuid.toString
      val u = phantomUsersDao.insert(PhantomUser(None, UUID.randomUUID, "email", "", LocalDate.now, true, "", Verified))
      Await.result(sessions.createSession(PhantomSession(uuid, u.id.get, sessionCreated, sessionCreated)), waitPeriod)
      val h = hashValues(d, s)
      val url = s"/users/logout?$hashP=$h&$dateP=$d&$sessionIdP=$s"
      Get(url) ~> userRoute ~> check {
        status === OK
        Get(s"/users/1/contacts?$hashP=$h&$dateP=$d&$sessionIdP=$s") ~> userRoute ~> check {
          rejection must beAnInstanceOf[AuthenticationFailedRejection]
        }
      }

    }
  }
}