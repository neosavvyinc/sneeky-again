package com.shoutout.ds.registration

import org.specs2.mutable.Specification
import com.shoutout.ds.{ TestUtils, PhantomEndpointSpec }
import spray.testkit.Specs2RouteTest
import com.shoutout.ds.framework.auth.PassThroughEntryPointAuthenticator
import com.shoutout.ds.dataAccess.BaseDAOSpec
import org.joda.time.LocalDate
import scala.concurrent.duration
import java.util.concurrent.TimeUnit
import com.shoutout.model._
import com.shoutout.model.RegistrationResponse
import spray.http.StatusCodes._
import spray.http.{ StatusCodes, FormData }
import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with PassThroughEntryPointAuthenticator
    with RegistrationEndpoint
    with BaseDAOSpec
    with TestUtils {

  def actorRefFactory = system

  sequential

  "Registration Service" should {

    "Return a simple hello message" in withSetupTeardown {

      Get("/registration") ~> registrationRoute ~> check {
        status == StatusCodes.OK
      }
    }

  }
}