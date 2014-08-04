package com.phantom.ds.user

import com.phantom.model._

import org.specs2._
import mutable.Specification
import spray.testkit.Specs2RouteTest
import com.phantom.ds.PhantomEndpointSpec
import org.joda.time.LocalDate
import com.phantom.ds.framework.auth.{ SuppliedUserRequestAuthenticator, PassThroughEntryPointAuthenticator }
import com.phantom.ds.dataAccess.BaseDAOSpec
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

    //TODO: get requirements around multiple logon.
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
    }.pendingUntilFixed("For now they don't want this as it is causing an issue on multiple devices")

    "identify service should yield a sanitized object of the user" in withSetupTeardown {
      val u = createVerifiedUser("adamparrish@something.com", "mypassword")
      authedUser = Some(u)
      val session = await(sessions.createSession(PhantomSession.newSession(u)))(ec)

      Get(s"/users/active?sessionId=${session.sessionId.toString}") ~> userRoute ~> check {
        assertPayload[SanitizedUser] { response =>
          response.uuid must be equalTo u.uuid
          // response.birthday must be equalTo u.birthday
          response.status must be equalTo u.status
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

      Post("/users/settings?sessionId=38400000-8cf0-11bd-b23e-10b96e4ef00d", SettingsRequest(
        false,
        SoundOnNewNotification
      )) ~> userRoute ~> check {
        status == StatusCodes.OK
      }
    }

    "Sending a push setting for mobile push type with a session id" in withSetupTeardown {

      authedUser = Some(createVerifiedUser("adam@somewheres.com", "anything"))

      Post("/users/settings?sessionId=38400000-8cf0-11bd-b23e-10b96e4ef00d", SettingsRequest(
        false,
        MutualContactMessaging
      )) ~> userRoute ~> check {
        status == StatusCodes.OK
      }
    }

    /*"Requesting a forgot password should accept the email address of the user" in withSetupTeardown {
      implicit val routeTestTimeout = RouteTestTimeout(duration.FiniteDuration(5, TimeUnit.SECONDS))
      Post("/users/forgotPassword", ForgotPasswordRequest(
        "aparrish@neosavvy.com"
      )) ~> userRoute ~> check {
        status == StatusCodes.OK
      }

    }*/

    "be able to upload a contact list which does not delete blocked users and returns a list of contacts which excludes blocked users who are not in their contacts" in withSetupTeardown {
      insertTestUsers()
      val user = createVerifiedUser("1@1.com", "password", "777777")
      authedUser = Some(user)
      contacts.insertAll(Seq(Contact(None, user.id.get, 1, Blocked), Contact(None, user.id.get, 2, Blocked), Contact(None, user.id.get, 3, Friend)))
      val contactsToUpload = Map("numbers" -> Seq("111111", "444444", "555555", "666666", "999999"))
      val expectedNumbers = Set(Some("111111"), Some("444444"), Some("555555"), Some("666666"))
      val expectedBlockedNumbers = Set(Some("111111"), Some("222222"))
      val expectedFriendNumbers = Set(Some("444444"), Some("555555"), Some("666666"))
      Post("/users/contacts", contactsToUpload) ~> userRoute ~> check {
        assertPayload[Seq[SanitizedContact]] { response =>

          val set = response.toSet
          set.foreach { x => expectedNumbers.find(_ == x.phoneNumber) must beSome }
          response must have size 4
        }
      }

      val allContacts = contacts.findAllForOwner(user.id.get).toSet
      expectedBlockedNumbers.foreach { x =>
        val found = allContacts.find(y => y._2.phoneNumber == x)
        found must beSome
        found.get._1.contactType must be equalTo Blocked
      }

      expectedFriendNumbers.foreach { x =>
        val found = allContacts.find(y => y._2.phoneNumber == x)
        found must beSome
        found.get._1.contactType must be equalTo Friend
      }

      allContacts must have size 5

    }

    /*"be able to block users" in withSetupTeardown {
      insertTestUsers()
      val user = createVerifiedUser("1@1.com", "password", "777777")
      authedUser = Some(user)
      contacts.insertAll(Seq(Contact(None, user.id.get, 1, Blocked), Contact(None, user.id.get, 2, Blocked), Contact(None, user.id.get, 3, Friend), Contact(None, user.id.get, 4, Friend)))
      val contactsToBlock = Seq("111111", "333333", "666666", "999999")
      val expectedBlockedNumbers = Set(Some("111111"), Some("222222"), Some("333333"), Some("666666"))
      val expectedFriendNumbers = Set(Some("444444"))
      Post("/users/block", contactsToBlock) ~> userRoute ~> check {
        status == StatusCodes.OK
      }

      val allContacts = contacts.findAllForOwner(user.id.get).toSet
      expectedBlockedNumbers.foreach { x =>
        val found = allContacts.find(y => y._2.phoneNumber == x)
        found must beSome
        found.get._1.contactType must be equalTo Blocked
      }

      expectedFriendNumbers.foreach { x =>
        val found = allContacts.find(y => y._2.phoneNumber == x)
        found must beSome
        found.get._1.contactType must be equalTo Friend
      }

      allContacts must have size 5

    }*/

    "clear a blocked list" in withSetupTeardown {
      insertTestUsers()
      val user = createVerifiedUser("email@email.com", "anything")
      authedUser = Some(user)
      val c = Seq(Contact(None, user.id.get, 1, Friend), Contact(None, user.id.get, 2, Friend), Contact(None, user.id.get, 3, Blocked), Contact(None, user.id.get, 4, Blocked))
      contacts.insertAll(c)
      Post("/users/clearblocklist") ~> userRoute ~> check {
        status == StatusCodes.OK

        val (notBlocked, blocked) = contacts.findAllForOwner(user.id.get).partition(_._1.contactType == Friend)
        notBlocked must have size 2
        blocked must beEmpty
      }

    }
  }

}
