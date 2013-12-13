package com.phantom.ds.user

import com.phantom.model._
import com.phantom.ds.framework.httpx._
import spray.http.StatusCodes._
import org.specs2._
import mutable.Specification
import specification.{ Before, After }
import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging
import com.phantom.ds.PhantomEndpointSpec

object clearMap extends Before {
  def before {
    MapbackedUserService.map.clear
    MapbackedUserService.contactList = List[String]()
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
  // can we get rid of this???
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

    "return an empty list of contacts if a user does not have any" in clearMap {
      val newUser = UserRegistration("ccaplinger@neosavvy.com", "10/12/1986", "mypassword")
      Post("/users/register", newUser) ~> userRoute ~> check {
        status == OK
      }

      Get("/users/1/contacts") ~> userRoute ~> check {
        status == OK
        assertPayload[UserResponse] { response =>
          response.code must be equalTo 200
          response.message must be equalTo """[]"""
        }
      }
    }

    "return a NonexistantUserException if you try to update contacts for a nonexistant user" in clearMap {
      val phoneNumbers = List("(614)499-3676", "(614)519-2050", "(614)206-1266")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        assertFailure(103);
      }
    }

    "be able to update your contacts with a list of phone numbers" in clearMap {
      val newUser = UserRegistration("adamparrish@something.com", "8/10/1981", "somethingelse")
      Post("/users/register", newUser) ~> userRoute ~> check {
        status == OK
      }

      val phoneNumbers = List("(614)499-3676", "(614)519-2050", "(614)206-1266")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        assertPayload[UserResponse] { response =>
          response.code must be equalTo 200
          response.message must be equalTo """["(614)499-3676","(614)519-2050","(614)206-1266"]"""
        }
      }
    }

    "be able to get a user's contacts" in clearMap {
      val newUser = UserRegistration("ccaplinger@neosavvy.com", "10/12/1986", "mypassword")
      Post("/users/register", newUser) ~> userRoute ~> check {
        status == OK
      }

      val phoneNumbers = List("614-499-3676", "614-519-2050")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        status == OK
      }

      Get("/users/1/contacts") ~> userRoute ~> check {
        status == OK
        assertPayload[UserResponse] { response =>
          response.code must be equalTo 200
          response.message must be equalTo """["614-499-3676","614-519-2050"]"""
        }
      }
    }

    "fail to find an unregistered user" in clearMap {
      Get("/users/1") ~> userRoute ~> check {
        assertFailure(103)
      }
    }

    "be able to get a registered user" in clearMap {
      val newUser = UserRegistration("ccaplinger@neosavvy.com", "10/12/1986", "mypassword")
      Post("/users/register", newUser) ~> userRoute ~> check {
        status == OK
      }

      Get("/users/1") ~> userRoute ~> check {
        assertPayload[UserResponse] { response =>
          response.code must be equalTo 200
          response.message must be equalTo """{"id":1,"email":"ccaplinger@neosavvy.com","birthday":"1/1/01","active":true}"""
        }
      }
    }

  }

}
