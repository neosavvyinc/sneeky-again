package com.phantom.ds.user

import com.phantom.model._
import com.phantom.model.UserJsonImplicits._
import spray.http.StatusCodes._

import org.specs2.mutable.Specification

import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging

class UserEndpointSpec extends Specification with Specs2RouteTest with Logging with UserEndpoint {

  def actorRefFactory = system

  "User Service" should {
    "be able to register a user" in {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "mypassword")
      Post("/user/register", newUser) ~> userRoute ~> check {
        status == OK
      }
    }

    "be able to log in a user " in {
      val newUser = UserLogin("adamparrish@something.com", "mypassword")
      Post("/user/login", newUser) ~> userRoute ~> check {
        status == OK
      }
    }

    "be able to get a user profile" in {
      Get("/user/profile/1") ~> userRoute ~> check {
        status == OK
      }
    }

    "fail if registering a user with a duplicate email" in {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "somethingelse")
      Post("/registration", newUser) ~> userRoute ~> check {
        status == OK
      }
      Post("/registration", newUser) ~> userRoute ~> check {
        status == InternalServerError
      }
    }.pendingUntilFixed()
  }

}
