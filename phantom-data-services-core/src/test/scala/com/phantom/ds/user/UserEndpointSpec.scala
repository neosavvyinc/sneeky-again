package com.phantom.ds.user

import com.phantom.model._
import com.phantom.ds.framework.httpx._

import org.specs2._
import mutable.Specification
import spray.testkit.Specs2RouteTest
import com.phantom.ds.PhantomEndpointSpec
import org.joda.time.LocalDate
import com.phantom.ds.framework.auth.{ SuppliedUserRequestAuthenticator, PassThroughEntryPointAuthenticator, PassThroughRequestAuthenticator }
import com.phantom.ds.dataAccess.BaseDAOSpec
import spray.http.StatusCodes
import java.util.UUID

class UserEndpointSpec extends Specification
    with PhantomEndpointSpec
    with Specs2RouteTest
    with SuppliedUserRequestAuthenticator
    with PassThroughEntryPointAuthenticator
    with UserEndpoint
    with BaseDAOSpec {

  def actorRefFactory = system

  val birthday = LocalDate.parse("1981-08-10")

  sequential

  "User Service" should {

    "fail logging in if a user does not exist" in withSetupTeardown {
      val newUser = UserLogin("crazy@abc.xyz", "mypassword")
      Post("/users/login", newUser) ~> userRoute ~> check {
        assertFailure(103)
      }
    }

    "log in should succeed for an unverified user" in withSetupTeardown {
      val login = UserLogin("nsauro@ev.com", "password")
      createUnverifiedUser("nsauro@ev.com", "password")
      Post("/users/login", login) ~> userRoute ~> check {
        assertPayload[LoginSuccess] { response =>
          response.sessionUUID must not be null
        }
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

    "sending a push notifier up with a session id to save to a session" in withSetupTeardown {

      authedUser = Some(createVerifiedUser("adam@somewheres.com", "anything"))

      Post("/users/pushNotifier?sessionId=38400000-8cf0-11bd-b23e-10b96e4ef00d", UpdatePushTokenRequest(
        "anysessionid",
        Apple
      )) ~> userRoute ~> check {
        status == StatusCodes.OK
      }
    }

    "Sending a push setting for sound toggle with a session id" in withSetupTeardown {

      authedUser = Some(createVerifiedUser("adam@somewheres.com", "anything"))

      Post("/users/pushSettings?sessionId=38400000-8cf0-11bd-b23e-10b96e4ef00d", PushSettingsRequest(
        false,
        SoundOnNewNotification
      )) ~> userRoute ~> check {
        status == StatusCodes.OK
      }
    }

    "clear a blocked list" in withSetupTeardown {
      insertTestUsers()
      val user = createVerifiedUser("email@email.com", "anything")
      authedUser = Some(user)
      val c = Seq(Contact(None, user.id.get, 1, Friend), Contact(None, user.id.get, 2, Friend), Contact(None, user.id.get, 3, Blocked), Contact(None, user.id.get, 4, Blocked))
      contacts.insertAll(c)
      Post("/users/clearblocklist") ~> userRoute ~> check {
        status == StatusCodes.OK

        val fetched = contacts.findAllForOwner(user.id.get)
        val notBlocked = fetched.filter(_.contactType == Friend)
        notBlocked must have size 4
      }

    }
  }

}
