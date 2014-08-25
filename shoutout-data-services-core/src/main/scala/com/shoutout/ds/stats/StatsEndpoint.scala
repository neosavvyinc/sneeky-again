package com.shoutout.ds.stats

import com.shoutout.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.shoutout.ds.framework.httpx.PhantomJsonProtocol
import com.shoutout.ds.{ BasicCrypto, DataHttpService }
import com.shoutout.model.StatsRequest
import spray.http.MediaTypes._

/**
 * Created by Dave on 8/25/14.
 */

trait StatsEndpoint extends DataHttpService with PhantomJsonProtocol with BasicCrypto {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  val statsService = StatsService()
  val stats = "stats"

  def getStats = pathPrefix(stats / "stats") {
    post {
      entity(as[StatsRequest]) { statRequest =>
        respondWithMediaType(`application/json`) {
          complete(statsService.getStats(statRequest))
        }
      }
    }
  }

  val statsRoute = getStats
}
