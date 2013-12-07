package com.currant.ds.game

import org.specs2.mutable.{ Around, Specification }
import org.specs2.specification.{ BeforeExample, Scope }

import spray.http.StatusCodes._
import spray.testkit.Specs2RouteTest
import com.currant.model._
import com.currant.ds.{ DBAwareBaseServiceSpec, DSConfiguration }
import spray.json._
import DefaultJsonProtocol._
import com.currant.ds.db.DB
import com.jolbox.bonecp.{ BoneCP, BoneCPConfig }

/**
 * Created with IntelliJ IDEA.
 * User: christopher
 * Date: 11/21/13
 * Time: 8:49 PM
 */

object GameEnpointProtocol extends DefaultJsonProtocol {
  implicit val gameFormat = jsonFormat6(Game)
}

class GameEndpointSpec extends DBAwareBaseServiceSpec with GameEndpoint {

  sequential

  import GameEnpointProtocol.gameFormat

  "GameService" should {
    "return the requested games" in {
      val testGame : Game = Game(1, "calvinball, no rules", true, None, None, None)

      Get("/games/1") ~> gameRoute ~> check {
        val gamesString = responseAs[String]
        val game = gamesString.asJson.convertTo[Game]

        game must be equalTo (testGame)
        status === OK
      }
    }
    // TO DO
    // change to Option[Game] and test if => None

    "return all games" in {
      val testGames =
        Seq(
          Game(1, "calvinball, no rules", true, None, None, None),
          Game(2, "foosball", true, None, None, None)
        )

      Get("/games") ~> gameRoute ~> check {
        val gamesString = responseAs[String]
        val games = gamesString.asJson.convertTo[Seq[Game]]

        games must be equalTo (testGames)
        status === OK
      }
    }
  }
}
