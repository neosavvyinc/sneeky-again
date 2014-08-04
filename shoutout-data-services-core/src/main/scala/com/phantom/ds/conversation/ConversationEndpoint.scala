package com.phantom.ds.conversation

import spray.http.MediaTypes._
import com.phantom.ds.{ BasicCrypto, DataHttpService }
import com.phantom.model.{ ConversationStartRequest, ConversationRespondRequest }
import com.phantom.ds.framework.auth.RequestAuthenticator
import akka.actor.ActorRef
import com.phantom.model.Paging
import com.phantom.ds.integration.amazon.S3Service
import spray.http.StatusCodes

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:37 PM
 */
trait ConversationEndpoint extends DataHttpService with BasicCrypto {
  this : RequestAuthenticator =>

  def appleActor : ActorRef
  def s3Service : S3Service

  val conversationService = ConversationService(appleActor, s3Service)
  val conversation = "conversation"

  val conversationRoute = pathPrefix(conversation) {
    get {
      respondWithMediaType(`application/json`) {
        complete {
          StatusCodes.OK
        }
      }
    }
  }
}
