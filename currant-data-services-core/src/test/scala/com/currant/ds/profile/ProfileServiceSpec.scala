package com.currant.ds.profile

import com.currant.ds.DBAwareBaseServiceSpec
import com.currant.model._
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.http.StatusCodes._
import CurrantUserJsonImplicits._
import com.currant.model.Profile

/**
 * Created by Neosavvy - test1
 *
 * User: adamparrish
 * Date: 11/21/13
 * Time: 9:35 PM
 */
object ProfileServiceSpec extends DBAwareBaseServiceSpec with ProfileEndpoint {

  sequential

  "ProfileService" should {
    "support finding a profile by an integer id" in {

      Get("/profile/1") ~> profileRoute ~> check {
        status == OK
        val profile = responseAs[Profile]
        profile.id must be equalTo 1
        profile.preferredTime must be equalTo EarlyMorning
        profile.profileLevel must be equalTo Elite
        profile.source must be equalTo Currant
      }
    }
  }

}
