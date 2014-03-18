package com.phantom.ds.photo

import com.phantom.model._

import org.specs2._
import mutable.Specification
import spray.testkit.Specs2RouteTest
import com.phantom.ds.PhantomEndpointSpec
import org.joda.time.LocalDate
import com.phantom.ds.framework.auth.{ SuppliedUserRequestAuthenticator, PassThroughEntryPointAuthenticator }
import com.phantom.ds.dataAccess.BaseDAOSpec
import spray.http.StatusCodes
import scala.concurrent.duration
import java.util.concurrent.TimeUnit

class PhotoEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with SuppliedUserRequestAuthenticator
    with PassThroughEntryPointAuthenticator
    with PhotoEndpoint
    with BaseDAOSpec {

  def actorRefFactory = system

  sequential

  "User Service" should {

    "should yield an object containing photos and categories" in withSetupTeardown {
      val u = createVerifiedUser("ccaplinger@dickpics.com", "mypassword")
      val session = await(sessions.createSession(PhantomSession.newSession(u)))(ec)

      authedUser = Some(u)
      Get(s"/photos?sessionId=${session.sessionId.toString}") ~> photoRoute ~> check {
        assertPayload[PhotoCategoryList] { response =>
          response.name must be equalTo "categories"
          response.photoList.size must be equalTo 0
        }
      }
    }

  }

}
