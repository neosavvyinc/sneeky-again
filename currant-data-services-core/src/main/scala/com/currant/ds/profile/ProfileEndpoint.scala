package com.currant.ds.profile

import spray.http._
import MediaTypes._

import com.currant.ds.DataHttpService
import com.currant.model.CurrantUserJsonImplicits
import spray.json._

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 11/21/13
 * Time: 9:36 PM
 */
trait ProfileEndpoint extends DataHttpService {

  import CurrantUserJsonImplicits._

  val profileDataService = ProfileService(db)

  //  ~
  //
  //    /**
  //     *
  //     */
  //    path("extended" / IntNumber) { id =>
  //      get {
  //        respondWithMediaType(`application/json`) {
  //          complete {
  //            val profileObject = profileDataService.findExtendedProfile( id )
  //            swrite(profileObject)
  //          }
  //        }
  //
  //      }
  //    }

  val profileRoute =
    pathPrefix("profile") {

      /**
       *
       */
      path(IntNumber) { id =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              profileDataService.findProfile(id)
            }
          }
        }
      }

    }

}
