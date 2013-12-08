package com.phantom.ds.conversation

import spray.http.MediaTypes._
import com.phantom.ds.DataHttpService
import com.phantom.model.{ ConversationSummary }
import spray.http.StatusCodes

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:37 PM
 */
trait ConversationEndpoint extends DataHttpService {

  val conversationService = ConversationService()
  val conversation = "conversation"

  val conversationRoute =

    pathPrefix(conversation) {
      path(IntNumber) {
        id =>
          get {
            respondWithMediaType(`application/json`) {
              complete {
                conversationService.findFeed(id)
              }
            }
          }
      }
    }

}
