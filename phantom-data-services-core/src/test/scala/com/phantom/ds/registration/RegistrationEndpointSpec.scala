package com.phantom.ds.registration

import org.specs2.mutable.Specification
import com.phantom.ds.{ TestUtils, PhantomEndpointSpec }
import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.auth.PassThroughEntryPointAuthenticator
import com.phantom.ds.dataAccess.BaseDAOSpec
import org.joda.time.LocalDate
import scala.concurrent.{ Await, duration }
import java.util.concurrent.TimeUnit
import com.phantom.model._
import scala.concurrent.duration._
import com.phantom.model.RegistrationResponse
import com.phantom.model.UserRegistration
import spray.http.StatusCodes._

class RegistrationEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with PassThroughEntryPointAuthenticator
    with RegistrationEndpoint
    with BaseDAOSpec
    with TestUtils {

  def actorRefFactory = system

  val birthday = LocalDate.parse("1981-08-10")

  sequential

  "Registration Service" should {

    "be able to register a user" in withSetupTeardown {

      implicit val routeTestTimeout = RouteTestTimeout(duration.FiniteDuration(5, TimeUnit.SECONDS))

      val newUser = UserRegistration("adamparrish@something.com", birthday, "mypassword")
      Post("/users/register", newUser) ~> registrationRoute ~> check {
        assertPayload[RegistrationResponse] { response =>
          response.sessionUUID must not be null
          response.verificationUUID must not be null
        }
      }
    }

    "fail if registering a user with a duplicate email" in withSetupTeardown {
      val newUser = UserRegistration("adamparrish@something.com", birthday, "somethingelse")
      createVerifiedUser(newUser.email, newUser.password)

      Post("/users/register", newUser) ~> registrationRoute ~> check {
        assertFailure(101)
      }
    }

    "fail if registering user doesn't meet password complexity" in withSetupTeardown {
      val newUser = UserRegistration("adamparrish@something.com", birthday, "s")
      Post("/users/register", newUser) ~> registrationRoute ~> check {
        assertFailure(105)
      }
    }

    "be able to verify a registration" in withSetupTeardown {

      val user = createUnverifiedUser("email@email.com", "password")
      user.id must not beNone
      val regResponse = reg("pre", user.uuid.toString, "post")

      Post("/users/verification", regResponse) ~> registrationRoute ~> check {
        status == OK
        val updatedUser = Await.result(phantomUsersDao.find(user.id.get), FiniteDuration(5, SECONDS))
        updatedUser.status must be equalTo Verified
      }
    }.pendingUntilFixed("This will need to be updated to support form fields instead of json")

  }

}