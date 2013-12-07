package com.phantom.ds.user

import com.phantom.model.{ RegistrationResponse, UserRegistration }
import spray.http.StatusCodes._

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec

class UserEndpointSpec extends Specification with PhantomEndpointSpec with Specs2RouteTest with Logging with UserEndpoint {

  def actorRefFactory = system

  "User Service" should {
    "be able to register a user without facebook" in {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "somethingelse")
      Post("/registration", newUser) ~> userRoute ~> check {
        assertPayload[RegistrationResponse] {
          response =>
            response.code must be equalTo 200
            response.message must be equalTo newUser.email
        }
      }
    }

    "fail if registering a user with a duplicate email" in {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "somethingelse")
      Post("/registration", newUser) ~> userRoute ~> check {
        status == OK
      }

      Post("/registration", newUser) ~> userRoute ~> check {
        assertFailure(101)
      }
    }
  }

}