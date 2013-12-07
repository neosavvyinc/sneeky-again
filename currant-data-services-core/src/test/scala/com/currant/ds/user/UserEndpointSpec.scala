package com.currant.ds.user

import com.currant.ds.DBAwareBaseServiceSpec
import com.currant.model.{ InsertResponse, CurrantUserRegistration }
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.http.StatusCodes._
import com.currant.model.CurrantUserJsonImplicits._

class UserEndpointSpec extends DBAwareBaseServiceSpec with UserEndpoint {

  sequential

  // override def dbScripts: Set[String] = Set("/sql/sport/register.sql")

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