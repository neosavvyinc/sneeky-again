package com.currant.ds.game

import spray.http._
import spray.json._
import MediaTypes._
import com.currant.model.{ Game, GameCreateRequest }

import com.currant.ds.DataHttpService

object GameEndpointProtocol extends DefaultJsonProtocol {
  implicit val gameFormat = jsonFormat6(Game)
  implicit val gameCreateFormat = jsonFormat5(GameCreateRequest)
}

trait GameEndpoint extends DataHttpService {

  import GameEndpointProtocol._

  val gameDataService = GameService(db)
  val games = "games"

  val gameRoute =
    path(games) {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            gameDataService.getAll
          }
        }
      } ~
        post {
          respondWithMediaType(`application/json`) {
            entity(as[GameCreateRequest]) { game =>
              complete {
                gameDataService.create(game)
              }
            }
          }
        }
    } ~
      path(games / IntNumber) { id =>
        get {
          respondWithMediaType(`application/json`) {
            complete(gameDataService.get(id))
          }
        } ~
          put {
            respondWithMediaType(`application/json`) {
              entity(as[GameCreateRequest]) { gameCreateRequest =>
                complete {
                  gameDataService.update(id, gameCreateRequest)
                }
              }
            }
          } ~
          delete {
            respondWithMediaType(`application/json`) {
              complete {
                gameDataService.delete(List(id))
                StatusCodes.OK
              }
            }
          }
      }
}
