package com.phantom.ds.conversation

import spray.http.MediaTypes._
import com.phantom.ds.DataHttpService

import com.phantom.ds.framework.auth.RequestAuthenticator
import akka.actor.ActorRef

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:37 PM
 */
trait ConversationEndpoint extends DataHttpService {
  this : RequestAuthenticator =>

  def twilioActor : ActorRef

  def appleActor : ActorRef

  val conversationService = ConversationService(twilioActor, appleActor) //need a better way of injecting services..trait!
  val conversation = "conversation"

  val conversationRoute =

    pathPrefix(conversation) {
      authenticate(request _) { user =>
        get {
          respondWithMediaType(`application/json`) {
            complete(
              conversationService.findFeed(user.id.get)
            )
          }
        }
      }
    } ~ {
      val ByteJsonFormat = null

      import spray.httpx.encoding.NoEncoding
      pathPrefix(conversation) {
        path("start") {
          authenticate(request _) { user =>
            post {
              formFields('image.as[Array[Byte]], 'imageText, 'toUsers.as[String]) { (image, imageText, toUsers) =>
                complete {
                  conversationService.startConversation(
                    user.id.get,
                    toUsers.split(",").toSet,
                    imageText,
                    //todo: move this into the service, and future bound it
                    conversationService.saveFileForConversationId(image, user.id.get)
                  )
                }
              }
            }
          }
        }
      }
    } ~ {
      val ByteJsonFormat = null

      import spray.httpx.encoding.{ NoEncoding, Gzip }
      //TODO are images required?
      pathPrefix(conversation) {
        path("respond") {
          authenticate(request _) { user =>
            post {
              formFields('image.as[Array[Byte]], 'imageText, 'convId.as[Long]) { (image, imageText, convId) =>
                complete {
                  conversationService.respondToConversation(
                    user.id.get,
                    convId,
                    imageText,
                    image)

                }
              }
            }
          }
        }
      }
    } ~ {
      pathPrefix(conversation) {
        path("block" / IntNumber) { id =>
          authenticate(request _) { user =>
            post {
              respondWithMediaType(`application/json`) {
                complete {
                  conversationService.blockByConversationId(user.id.get, id)
                }
              }
            }
          }
        }
      }
    }
}
