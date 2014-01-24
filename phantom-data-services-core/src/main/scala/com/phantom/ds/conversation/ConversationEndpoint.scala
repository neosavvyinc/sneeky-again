package com.phantom.ds.conversation

import spray.http.MediaTypes._
import com.phantom.ds.DataHttpService
import com.phantom.model._
import com.phantom.ds.framework.httpx._

import scala._
import scala.concurrent.{ Future, ExecutionContext }
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.phantom.model.BlockUserByConversationResponse

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:37 PM
 */
trait ConversationEndpoint extends DataHttpService {
  this : RequestAuthenticator =>

  val conversationService = ConversationService()
  val conversation = "conversation"

  val conversationRoute =

    pathPrefix(conversation) {
      path(IntNumber) {
        id =>
          get {
            respondWithMediaType(`application/json`) {
              complete(
                conversationService.findFeed(id)
              )
            }
          }
      }
    } ~ {
      val ByteJsonFormat = null

      import spray.httpx.encoding.{ NoEncoding, Gzip }

      pathPrefix(conversation) {
        path("start") {
          post {
            formFields('image.as[Array[Byte]], 'imageText, 'userid.as[Long], 'toUsers.as[String]) { (image, imageText, userid, toUsers) =>
              complete {
                conversationService.startConversation(
                  userid,
                  toUsers.split(",").toSet,
                  imageText,
                  //todo: move this into the service, and future bound it
                  conversationService.saveFileForConversationId(image, userid)
                )
              }
            }
          }
        }
      }
    } ~ {
      val ByteJsonFormat = null

      import spray.httpx.encoding.{ NoEncoding, Gzip }

      pathPrefix(conversation) {
        path("respond") {
          post {
            formFields('image.as[Array[Byte]], 'imageText, 'convId.as[Long]) { (image, imageText, convId) =>

              complete {
                conversationService.respondToConversation(
                  convId,
                  imageText,
                  conversationService.saveFileForConversationId(image, convId))

              }
            }
          }
        }
      }
    } ~ {
      pathPrefix(conversation) {
        path("block" / IntNumber) {
          id =>
            post {
              respondWithMediaType(`application/json`) {
                complete {
                  BlockUserByConversationResponse(1)
                }
              }
            }
        }
      }

    }

}
