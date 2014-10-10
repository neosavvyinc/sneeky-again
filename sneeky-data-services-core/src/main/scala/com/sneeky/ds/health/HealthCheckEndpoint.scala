package com.sneeky.ds.health

import com.sneeky.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.sneeky.ds.{ BasicCrypto, DataHttpService }
import com.sneeky.ds.framework.httpx.SneekyJsonProtocol
import spray.http.MediaTypes._
import spray.http.StatusCodes

/**
 * Created by aparrish on 8/19/14.
 */
trait HealthCheckEndpoint extends DataHttpService with SneekyJsonProtocol with BasicCrypto {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  val health = "health"

  def healthCheck = pathPrefix(health / "check") {
    get {
      respondWithMediaType(`application/json`) {
        complete {
          StatusCodes.OK
        }
      }
    }

  }

  val healthCheckRoute = healthCheck

}
