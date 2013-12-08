package com.phantom.ds.user

import com.phantom.model._
import com.phantom.model.UserJsonImplicits._
import spray.http.StatusCodes._
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec

class UserEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with Logging
    with UserEndpoint {

  def actorRefFactory = system

  "User Service" should {
    "be able to register a user" in {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "mypassword")
      Post("/users/register", newUser) ~> userRoute ~> check {
        assertPayload[UserResponse] { response =>
          response.code must be equalTo 200
          response.message must be equalTo newUser.email
        }
      }
    }

    "fail if registering a user with a duplicate email" in {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "somethingelse")
      Post("/users/register", newUser) ~> userRoute ~> check {
        status == OK
      }

      Post("/users/register", newUser) ~> userRoute ~> check {
        assertFailure(101)
      }
    }

    "be able to log in a user " in {
      val newUser = UserLogin("adamparrish@something.com", "mypassword")
      Post("/users/login", newUser) ~> userRoute ~> check {
        status == OK
        //        var res = responseAs[UserResponse]
        //        res.message must be equalTo ("test")
      }
    }

    //    "fail if an account does not exist" in {
    //      val newUser = UserLogin("adamparrish@something.com", "mypassword")
    //      Post("/users/login", newUser) ~> userRoute ~> check {
    //        status == OK
    //      }
    //    }

    "be able to get a user profile" in {
      Get("/users/1") ~> userRoute ~> check {
        status == OK
      }
    }

    "be able to get a user's friends" in {
      Get("/users/1/contacts") ~> userRoute ~> check {
        status == OK
      }
    }

    "be able to update your contacts with a list of phone numbers" in {
      val phoneNumbers = List("(614)499-3676", "(614)519-2050", "(614)206-1266")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        status == OK
      }
    }

  }

}
