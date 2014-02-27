package com.phantom.ds.conversation

import spray.http.MediaTypes._
import com.phantom.ds.DataHttpService

import com.phantom.ds.framework.auth.RequestAuthenticator
import akka.actor.ActorRef
import com.phantom.model.Paging

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
      authenticate(verified _) { user =>
        get {
          parameters('page.as[Int] ? -1, 'size.as[Int] ? -1) { (page, size) =>
            respondWithMediaType(`application/json`) {
              complete(
                //TODO: FeedFolder should also sanitize(maybe)
                conversationService.findFeed(user.id.get, Paging(page, size)) flatMap { resolvedFeed =>
                  conversationService.sanitizeFeed(resolvedFeed, user)
                }
              )
            }
          }
        }
      }
    } ~ {
      val ByteJsonFormat = null

      import spray.httpx.encoding.NoEncoding
      pathPrefix(conversation) {
        path("start") {
          authenticate(verified _) { user =>
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
      pathPrefix(conversation) {
        path("respond") {
          authenticate(verified _) { user =>
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
          authenticate(verified _) { user =>
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
    } ~ {
      pathPrefix(conversation) {
        path("view" / IntNumber) { id =>
          authenticate(verified _) { user =>
            post {
              respondWithMediaType(`application/json`) {
                complete {

                  val userId = user.id.get
                  log.debug(s"marking the $id conversation item as viewed for user $userId")

                  conversationService.viewConversationItem(id, userId)
                }
              }
            }
          }
        }
      } ~ {
        pathPrefix(conversation) {
          path("delete" / IntNumber) { id =>
            authenticate(verified _) { user =>
              delete {
                respondWithMediaType(`application/json`) {
                  complete {
                    conversationService.deleteConversation(user.id.get, id)
                  }
                }
              }
            }

          }
        }
      } ~ {
        pathPrefix(conversation) {
          path("deleteitem" / IntNumber) { id =>
            authenticate(verified _) { user =>
              delete {
                respondWithMediaType(`application/json`) {
                  complete {
                    conversationService.deleteConversationItem(user.id.get, id)
                  }
                }
              }
            }
          }
        }
      }
    }
}
