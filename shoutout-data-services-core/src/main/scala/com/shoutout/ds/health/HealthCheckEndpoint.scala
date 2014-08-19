package com.shoutout.ds.health

import com.shoutout.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.shoutout.ds.{ BasicCrypto, DataHttpService }
import com.shoutout.ds.framework.httpx.PhantomJsonProtocol
import spray.http.MediaTypes._
import spray.http.StatusCodes

/**
 * Created by aparrish on 8/19/14.
 */
trait HealthCheckEndpoint extends DataHttpService with PhantomJsonProtocol with BasicCrypto {
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
