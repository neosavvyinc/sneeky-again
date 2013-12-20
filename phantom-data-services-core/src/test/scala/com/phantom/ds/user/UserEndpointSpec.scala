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
import org.joda.time.LocalDate

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

  sequential

  def actorRefFactory = system

  val birthday = LocalDate.parse("1981-08-10")
  def registerUser(newUser : UserRegistration) = {
    Post("/users/register", newUser) ~> userRoute ~> check {
      status == OK
    }
  }

  sequential

  "User Service" should {

    "be able to register a user" in clearMap {
      val newUser = UserRegistration("adamparrish@something.com", birthday, "mypassword")
      Post("/users/register", newUser) ~> userRoute ~> check {
        assertPayload[ClientSafeUserResponse] { response =>
          response.email must be equalTo "adamparrish@something.com"
          response.birthday must be equalTo birthday
        }
      }
    }

    "fail if registering a user with a duplicate email" in clearMap {
      val newUser = UserRegistration("adamparrish@something.com", birthday, "somethingelse")
      registerUser(newUser)

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
      val newUser = UserRegistration("adamparrish@something.com", birthday, "mypassword")
      registerUser(newUser)

      Post("/users/login", UserLogin("adamparrish@something.com", "mypassword")) ~> userRoute ~> check {
        assertPayload[ClientSafeUserResponse] { response =>
          response.email must be equalTo "adamparrish@something.com"
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
      val newUser = UserRegistration("adamparrish@something.com", birthday, "somethingelse")
      registerUser(newUser)

      val phoneNumbers = List("6144993676", "6145192050", "6142061266")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        assertPayload[List[PhantomUser]] { response =>
          response.head.id must be equalTo "6144993676"
        }
      }
    }

    "be able to get a user's contacts" in clearMap {
      val newUser = UserRegistration("ccaplinger@neosavvy.com", birthday, "mypassword")
      registerUser(newUser)

      val phoneNumbers = List("6144993676", "6145192050")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        status == OK
      }

      Get("/users/1/contacts") ~> userRoute ~> check {
        assertPayload[List[PhantomUser]] { response =>
          response.head.id must be equalTo "6144993676"
        }
      }
    }

    "fail to find an unregistered user" in clearMap {
      Get("/users/1") ~> userRoute ~> check {
        assertFailure(103)
      }
    }

    "be able to get a registered user" in clearMap {
      val newUser = UserRegistration("ccaplinger@neosavvy.com", birthday, "mypassword")
      registerUser(newUser)

      Get("/users/1") ~> userRoute ~> check {
        assertPayload[ClientSafeUserResponse] { response =>
          response.email must be equalTo "ccaplinger@neosavvy.com"
        }
      }
    }

    "be able to clear the list of blocked users for a given user" in clearMap {
      val newUser = UserRegistration("adamparrish@something.com", birthday, "somethingelse")
      registerUser(newUser)

      Get("/users/1") ~> userRoute ~> check {
        assertPayload[ClientSafeUserResponse] { response =>
          response.email must be equalTo "adamparrish@something.com"
        }
      }
    }

  }

}
