package com.sneeky.ds.shoutout

import com.sneeky.ds.framework.exception.ShoutoutException
import spray.http.MediaTypes._
import com.sneeky.ds.{ BasicCrypto, DataHttpService }
import com.sneeky.model.{ ConversationStartRequest, ConversationRespondRequest }
import com.sneeky.ds.framework.auth.RequestAuthenticator
import akka.actor.ActorRef
import com.sneeky.model.Paging
import com.sneeky.ds.integration.amazon.S3Service
import spray.http.StatusCodes

import scala.concurrent.{ Future, future }

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:37 PM
 */
trait ShoutoutEndpoint extends DataHttpService with BasicCrypto {
  this : RequestAuthenticator =>

  def appleActor : ActorRef
  def s3Service : S3Service

  val shoutoutService = ShoutoutService(appleActor, s3Service)
  val shoutout = "shoutout"

  def sendAdminShoutout = pathPrefix(shoutout / "admin" / "send") {
    val ByteJsonFormat = null

    authenticate(admin _) { authenticationResult =>

      val (user, sessionId) = authenticationResult
      post {
        respondWithMediaType(`application/json`) {
          formFields('data.as[Array[Byte]], 'text.?, 'contentType, 'locale.?) {
            (data, text, contentType, locale) =>
              complete {

                contentType match {

                  case x if x == "video/quicktime" => shoutoutService.saveData(data, "video/quicktime").map { url =>
                    shoutoutService.sendToAll(user, url, text, contentType, locale)
                  }
                  case x if x == "video/mp4" => shoutoutService.saveData(data, "video/mp4").map { url =>
                    shoutoutService.sendToAll(user, url, text, contentType, locale)
                  }
                  case x if x == "audio/mp4" => shoutoutService.saveData(data, "audio/mp4").map { url =>
                    shoutoutService.sendToAll(user, url, text, contentType, locale)
                  }
                  case x if x == "image/jpg" => shoutoutService.saveData(data, "image/jpg").map { url =>
                    shoutoutService.sendToAll(user, url, text, contentType, locale)
                  }
                  case _ => future {
                    ShoutoutException.shoutoutContentTypeInvalid
                  }

                }
              }
          }
        }
      }

    }
  }

  def sendShoutout = pathPrefix(shoutout / "send") {
    val ByteJsonFormat = null
    authenticate(unverified _) {
      authenticationResult =>

        val (user, sessionId) = authenticationResult

        post {
          respondWithMediaType(`application/json`) {
            formFields('data.as[Array[Byte]], 'text.?, 'groupIds.?, 'friendIds.?, 'contentType) {
              (data, text, groupIds, friendIds, contentType) =>
                complete {

                  contentType match {

                    case x if x == "video/quicktime" => shoutoutService.saveData(data, "video/quicktime").map { url =>
                      shoutoutService.sendToRecipients(user, url, text, groupIds, friendIds, contentType)
                    }
                    case x if x == "video/mp4" => shoutoutService.saveData(data, "video/mp4").map { url =>
                      shoutoutService.sendToRecipients(user, url, text, groupIds, friendIds, contentType)
                    }
                    case x if x == "audio/mp4" => shoutoutService.saveData(data, "audio/mp4").map { url =>
                      shoutoutService.sendToRecipients(user, url, text, groupIds, friendIds, contentType)
                    }
                    case x if x == "image/jpg" => shoutoutService.saveData(data, "image/jpg").map { url =>
                      shoutoutService.sendToRecipients(user, url, text, groupIds, friendIds, contentType)
                    }
                    case _ => future {
                      ShoutoutException.shoutoutContentTypeInvalid
                    }

                  }
                }
            }
          }
        }
    }
  }

  def findShouts = pathPrefix(shoutout / "find") {
    authenticate(unverified _) { authenticationResult =>

      val (user, sessionId) = authenticationResult

      get {
        respondWithMediaType(`application/json`) {
          complete(shoutoutService.findAllForUser(user, sessionId))
        }
      }
    }
  }

  def setShoutAsViewed = pathPrefix(shoutout / "viewed" / IntNumber) { id =>
    authenticate(unverified _) { authenticationResult =>

      val (user, sessionId) = authenticationResult

      post {
        respondWithMediaType(`application/json`) {
          complete {
            shoutoutService.updateShoutoutAsViewedForUser(user, id)
          }
        }
      }
    }
  }

  val shoutoutRoute =
    sendShoutout ~
      findShouts ~
      setShoutAsViewed ~
      sendAdminShoutout
}
