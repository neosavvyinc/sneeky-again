package com.phantom.ds.integration.twilio

import akka.actor.ActorRef
import com.phantom.ds.DataHttpService
import com.phantom.ds.framework.httpx.PhantomJsonProtocol
import spray.http.StatusCodes._

trait TwilioEndpoint extends DataHttpService with PhantomJsonProtocol {

  def twilioActor : ActorRef

  val twilioRoute =
    pathPrefix("integration" / "invitation") {
      post {
        entity(as[InviteMessageStatus]) {
          reg =>
            complete {
              twilioActor ! reg
              OK
            }
        }
      }
    }

}