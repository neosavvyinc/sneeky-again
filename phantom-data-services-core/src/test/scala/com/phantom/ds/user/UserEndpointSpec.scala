package com.phantom.ds.user

import com.phantom.model._
import com.phantom.ds.framework.httpx._
import org.specs2._
import mutable.Specification
import spray.testkit.Specs2RouteTest
import com.phantom.ds.PhantomEndpointSpec
import org.joda.time.LocalDate
import com.phantom.ds.framework.auth.{ PassThroughEntryPointAuthenticator, PassThroughRequestAuthenticator }
import com.phantom.ds.dataAccess.BaseDAOSpec

class UserEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with PassThroughRequestAuthenticator
    with PassThroughEntryPointAuthenticator
    with UserEndpoint
    with BaseDAOSpec {

  def actorRefFactory = system

  val birthday = LocalDate.parse("1981-08-10")

  sequential

  "User Service" should {

    "be able to register a user" in withSetupTeardown {
      val newUser = UserRegistration("adamparrish@something.com", birthday, "mypassword")
      Post("/users/register", newUser) ~> userRoute ~> check {
        assertPayload[RegistrationResponse] { response =>
          response.sessionUUID must not be null
          response.verificationUUID must not be null
        }
      }
    }

    "fail if registering a user with a duplicate email" in withSetupTeardown {
      val newUser = UserRegistration("adamparrish@something.com", birthday, "somethingelse")
      createVerifiedUser(newUser.email, newUser.password)

      Post("/users/register", newUser) ~> userRoute ~> check {
        assertFailure(101)
      }
    }

    "fail logging in if a user does not exist" in withSetupTeardown {
      val newUser = UserLogin("crazy@abc.xyz", "mypassword")
      Post("/users/login", newUser) ~> userRoute ~> check {
        assertFailure(103)
      }
    }

    "fail logging in if a user is not verified" in withSetupTeardown {
      val login = UserLogin("nsauro@ev.com", "password")
      createUnverifiedUser("nsauro@ev.com", "password")
      Post("/users/login", login) ~> userRoute ~> check {
        assertFailure(104)
      }
    }

    "fail logging in if a password is not valid" in withSetupTeardown {
      val login = UserLogin("nsauro@ev.com", "badpassword")
      createVerifiedUser("nsauro@ev.com", "password")
      Post("/users/login", login) ~> userRoute ~> check {
        assertFailure(103)
      }
    }

    "log in if a user exists" in withSetupTeardown {
      createVerifiedUser("adamparrish@something.com", "mypassword")
      Post("/users/login", UserLogin("adamparrish@something.com", "mypassword")) ~> userRoute ~> check {
        assertPayload[LoginSuccess] { response =>
          response.sessionUUID must not be null
        }
      }
    }

    "logging more than once yields the same session" in withSetupTeardown {
      createVerifiedUser("adamparrish@something.com", "mypassword")
      Post("/users/login", UserLogin("adamparrish@something.com", "mypassword")) ~> userRoute ~> check {
        assertPayload[LoginSuccess] { response =>
          response.sessionUUID must not be null
          val uuid = response.sessionUUID
          Post("/users/login", UserLogin("adamparrish@something.com", "mypassword")) ~> userRoute ~> check {
            assertPayload[LoginSuccess] { res =>
              res.sessionUUID must be equalTo uuid
            }
          }
        }
      }
    }

    /*"return a NonexistantUserException if you try to update contacts for a nonexistant user" in withSetupTeardown {
      val phoneNumbers = List("(614)499-3676", "(614)519-2050", "(614)206-1266")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        assertFailure(103)
      }
    }

    "be able to update your contacts with a list of phone numbers" in withSetupTeardown {
      val newUser = UserRegistration("adamparrish@something.com", birthday, "somethingelse")
      registerUser(newUser)

      val phoneNumbers = List("6144993676", "6145192050", "6142061266")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        status == OK
        /*assertPayload[List[PhantomUser]] { response =>
          //response.head.id must be equalTo "6144993676"
        }*/
      }
    }

    "be able to get a user's contacts" in withSetupTeardown {
      val newUser = UserRegistration("ccaplinger@neosavvy.com", birthday, "mypassword")
      registerUser(newUser)

      val phoneNumbers = List("6144993676", "6145192050")
      Post("/users/1/contacts", phoneNumbers) ~> userRoute ~> check {
        status == OK
      }

      Get("/users/1/contacts") ~> userRoute ~> check {
        status == OK
        /*assertPayload[List[PhantomUser]] { response =>
          response.head.id must be equalTo "6144993676"
        }*/
      }
    }

    "fail to find an unregistered user" in withSetupTeardown {
      Get("/users/1") ~> userRoute ~> check {
        assertFailure(103)
      }
    }

    "be able to get a registered user" in withSetupTeardown {
      val newUser = UserRegistration("ccaplinger@neosavvy.com", birthday, "mypassword")
      registerUser(newUser)

      Get("/users/1") ~> userRoute ~> check {
        assertPayload[ClientSafeUserResponse] { response =>
          response.email must be equalTo "ccaplinger@neosavvy.com"
        }
      }
    }

    "be able to clear the list of blocked users for a given user" in withSetupTeardown {
      val newUser = UserRegistration("adamparrish@something.com", birthday, "somethingelse")
      registerUser(newUser)

      Get("/users/1") ~> userRoute ~> check {
        assertPayload[ClientSafeUserResponse] { response =>
          response.email must be equalTo "adamparrish@something.com"
        }
      }
    }*/

  }

}
