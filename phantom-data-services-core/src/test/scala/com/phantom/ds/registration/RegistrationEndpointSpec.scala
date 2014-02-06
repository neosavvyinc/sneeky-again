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
import spray.http.FormData
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http.{ BodyPart, MultipartFormData }

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

      val formData = FormData(Map("AccountSid" -> regResponse.accountSid,
        "MessageSid" -> regResponse.messageSid,
        "From" -> "987654321",
        "To" -> regResponse.to,
        "Body" -> regResponse.body,
        "NumMedia" -> regResponse.numMedia.toString))

      Post("/users/verification", formData) ~> registrationRoute ~> check {
        status == OK
        val updatedUser = Await.result(phantomUsersDao.find(user.id.get), FiniteDuration(5, SECONDS))
        updatedUser.status must be equalTo Verified
        updatedUser.phoneNumber must be equalTo "987654321"
      }
    }

    "be able to convert a StubUser" in withSetupTeardown {
      val fromUser = createVerifiedUser("n@n.com", "password").id.get
      val user = createUnverifiedUser("email@email.com", "password")
      val stubUser = await(stubUsersDao.insertAll(Seq(StubUser(None, "987654321", 0))))
      await(stubConversationsDao.insertAll(Seq(StubConversation(None, fromUser, stubUser.head.id.get, "text", "url"))))
      val regResponse = reg("pre", user.uuid.toString, "post")

      val formData = FormData(Map("AccountSid" -> regResponse.accountSid,
        "MessageSid" -> regResponse.messageSid,
        "From" -> "987654321",
        "To" -> regResponse.to,
        "Body" -> regResponse.body,
        "NumMedia" -> regResponse.numMedia.toString))

      Post("/users/verification", formData) ~> registrationRoute ~> check {
        status == OK
        val updatedUser = await(phantomUsersDao.find(user.id.get))
        updatedUser.status must be equalTo Verified
        updatedUser.phoneNumber must be equalTo "987654321"
        val stubUsers = await(stubUsersDao.findByPhoneNumbers(Set("987654321")))
        stubUsers must beEmpty
        val stubConversations = await(stubConversationsDao.findByFromUserId(fromUser))
        stubConversations must beEmpty

        val conversations = conversationDao.findConversationsAndItems(fromUser)
        conversations.foreach {
          case (c, items) =>
            items must have size 1
            items.head.imageText must be equalTo "text"
            items.head.imageUrl must be equalTo "url"
            c.fromUser must be equalTo fromUser
            c.toUser must be equalTo user.id.get
        }
        conversations must have size 1
      }
    }
  }
}