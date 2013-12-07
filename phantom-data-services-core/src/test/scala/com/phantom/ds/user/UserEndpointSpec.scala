package com.phantom.ds.user

import com.phantom.model.{ InsertResponse, CurrantUserRegistration }
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.http.StatusCodes._
import com.phantom.model.CurrantUserJsonImplicits._

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeExample

import spray.testkit.Specs2RouteTest
import com.phantom.ds.framework.Logging

class UserEndpointSpec extends Specification with Specs2RouteTest with Logging with UserEndpoint {

  def actorRefFactory = system

  "User Service" should {
    "be able to register a user without facebook" in {
      val newUser = CurrantUserRegistration("guy", "awesome", "maxpayne", "guy@johnson.com", "now", Seq.empty, None)
      Post("/registration", newUser) ~> userRoute ~> check {
        status == OK
        val response = responseAs[InsertResponse]
        response.profileId must be equalTo 1
        response.userId must be equalTo 1
      }

    }

    "fail if registering a user with a duplicate email" in {
      val newUser = CurrantUserRegistration("guy", "awesome", "maxpayne", "guy@johnson.com", "now", Seq.empty, None)
      Post("/registration", newUser) ~> userRoute ~> check {
        status == OK
      }
      Post("/registration", newUser) ~> userRoute ~> check {
        status == InternalServerError
      }
    }
  }

}