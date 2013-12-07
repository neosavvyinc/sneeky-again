package com.currant.ds.sport

import com.currant.model._
import com.currant.ds.DBAwareBaseServiceSpec
import spray.json._
import spray.http.StatusCodes._
import com.currant.model.SportJsonImplicits._

class SportEndpointSpec extends DBAwareBaseServiceSpec with SportEndpoint {

  sequential

  "SportService" should {
    "support inserting a new sport" in {
      val testSport = sportCreateReq("Baseball", "With a bat")
      Put("/sports", testSport) ~> sportRoute ~> check {
        status == OK
        val sport = responseAs[Sport]
        sport.name must be equalTo testSport.name
      }
    }

    "return a list of two sport= baseball and soccer" in {

      val testSport = sportCreateReq("Soccer", "With a ball")
      val testSport1 = sportCreateReq("Badminton", "Racquets")

      Put("/sports", testSport) ~> sportRoute ~> check {}
      Put("/sports", testSport1) ~> sportRoute ~> check {}

      Get("/sports") ~> sportRoute ~> check {
        status == OK
        val sports = responseAs[List[Sport]]
        sports must have size 2

      }
    }

    /* "allow a new sport parameter to be posted in" in {
       val testSport = sportCreateReq("Target Practice", "With them thar guns")
       Put("/sports", swrite(testSport)) ~> sportRoute ~> check {}
 
       val testSportUpdate = Sport(1, "new name", "new descript", true, None, None, None, None)
 
       Post("/sports", swrite(testSportUpdate)) ~> sportRoute ~> check {}
 
       Get("/sports/1") ~> sportRoute ~> check {
         val resp = responseAs[String]
         val sport = read[Sport](resp)
         sport must be equalTo testSportUpdate
       }
 
     }*/

    "allow a get with id to return one sport that matches the id" in {
      val testSport = sportCreateReq("Target Practice", "With them thar guns")
      Put("/sports", testSport) ~> sportRoute ~> check {}

      Get("/sports/1") ~> sportRoute ~> check {
        status == OK
        val sport = responseAs[Sport]
        sport.name must be equalTo testSport.name
      }
    }

    //TODO:  how to handle inactive sports?
    "return a 404 if requesting a sport that is not found" in {
      Get("/sports/1000") ~> sportRoute ~> check {
        status == NotFound
      }
    }

    "support a delete which should simply return ok" in {
      val testSport = sportCreateReq("Target Practice", "With them thar guns")
      Put("/sports", testSport) ~> sportRoute ~> check {}
      Delete("/sports/1") ~> sportRoute ~> check {
        status == OK
      }
    }
  }

  private def sportCreateReq(name : String, descrip : String) = SportCreateRequest(name, descrip, true, None, None, None, None)

}