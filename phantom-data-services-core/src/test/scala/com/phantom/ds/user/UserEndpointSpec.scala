package com.phantom.ds.user

import com.phantom.model._
import com.phantom.ds.framework.httpx._
import spray.http.StatusCodes._
import org.specs2._
import mutable.Specification
import specification.Before
import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec

object clearMap extends Before {
  def before {
    MapbackedUserService.map.clear
  }
}

class UserEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with Logging
    with PhantomJsonProtocol
    with UserEndpoint {

  def actorRefFactory = system

  // NOTE: making these tests sequential for now
  // while they depend on the mutable Mapbacked Service
  sequential

  "User Service" should {

    "be able to register a user" in clearMap {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "mypassword")
      Post("/users/register", newUser) ~> userRoute ~> check {
        assertPayload[UserResponse] { response =>
          response.code must be equalTo 200
          response.message must be equalTo newUser.email
        }
      }
    }

    "fail if registering a user with a duplicate email" in clearMap {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "somethingelse")
      Post("/users/register", newUser) ~> userRoute ~> check {
        status == OK
      }

      Post("/users/register", newUser) ~> userRoute ~> check {
        assertFailure(101)
      }
    }

    "fail logging in if a user does not exist" in clearMap {
      val newUser = UserLogin("adamparrish@something.com", "mypassword")
      Post("/users/login", newUser) ~> userRoute ~> check {
        assertFailure(103)
      }
    }

    "log in if a user exists" in clearMap {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "mypassword")
      Post("/users/register", newUser) ~> userRoute ~> check {
        status == OK
      }

      Post("/users/login", UserLogin("adamparrish@something.com", "mypassword")) ~> userRoute ~> check {
        assertPayload[UserResponse] { response =>
          response.code must be equalTo 200
          response.message must be equalTo "logged in!"
        }(userResponseFormat)
      }
    }

    "be able to get a user profile" in clearMap {
      Get("/users/1") ~> userRoute ~> check {
        status == OK
      }
    }

    "be able to get a user's friends" in clearMap {
      Get("/users/1/contacts") ~> userRoute ~> check {
        status == OK
      }
    }

    "be able to update your contacts with a list of phone numbers" in clearMap {
      val phoneNumbers = List("(614)499-3676", "(614)519-2050", "(614)206-1266")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        status == OK
      }
    }

    "fail to find an unregistered user" in clearMap {
      Get("/users/1") ~> userRoute ~> check {
        assertFailure(103)
      }
    }

  }

}
