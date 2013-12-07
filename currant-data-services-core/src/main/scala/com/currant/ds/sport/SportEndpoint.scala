package com.currant.ds.sport

import spray.http._
import MediaTypes._
import com.currant.model.{ SportJsonImplicits, Sport, SportCreateRequest }
import spray.json._
import com.currant.ds.DataHttpService

// this trait defines our service behavior independently from the service actor
trait SportEndpoint extends DataHttpService {

  import SportJsonImplicits._

  val sportDataService = SportService(db)

  val sportRoute =
    path("sports") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            sportDataService.getAll
          }
        }
      } ~
        post {
          respondWithMediaType(`application/json`) {
            entity(as[Sport]) { sport =>
              complete {
                sportDataService.update(sport)
              }
            }
          }
        } ~
        put {
          respondWithMediaType(`application/json`) {
            entity(as[SportCreateRequest]) { sport =>
              complete {
                sportDataService.create(sport)
              }
            }
          }
        }
    } ~
      pathPrefix("sports" / IntNumber) { id =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              sportDataService.get(id)
            }
          }
        } ~
          delete {
            respondWithMediaType(`application/json`) {
              complete {
                sportDataService.delete(List(id))
                StatusCodes.OK
              }
            }
          }
      }

}