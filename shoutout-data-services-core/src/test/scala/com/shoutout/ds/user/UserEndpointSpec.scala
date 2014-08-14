package com.shoutout.ds.user

import com.shoutout.ds.integration.amazon.S3Service
import com.shoutout.ds.integration.mock.TestS3Service
import com.shoutout.model._

import org.specs2._
import mutable.Specification
import spray.testkit.Specs2RouteTest
import com.shoutout.ds.PhantomEndpointSpec
import org.joda.time.LocalDate
import com.shoutout.ds.framework.auth.{ SuppliedUserRequestAuthenticator, PassThroughEntryPointAuthenticator }
import com.shoutout.ds.dataAccess.BaseDAOSpec
import spray.http.StatusCodes

class UserEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with SuppliedUserRequestAuthenticator
    with PassThroughEntryPointAuthenticator
    with UserEndpoint
    with BaseDAOSpec {

  def actorRefFactory = system

  val birthday = LocalDate.parse("1981-08-10")

  def s3Service : S3Service = new TestS3Service()

  sequential

  "User Service" should {

    "return a simple hello message" in withSetupTeardown {
      Get("/users") ~> userRoute ~> check {
        status == StatusCodes.OK
      }
    }

  }

}
