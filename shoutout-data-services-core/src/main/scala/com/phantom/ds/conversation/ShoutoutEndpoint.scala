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
trait ShoutoutEndpoint extends DataHttpService with BasicCrypto {
  this : RequestAuthenticator =>

  def appleActor : ActorRef
  def s3Service : S3Service

  val shoutoutService = ShoutoutService(appleActor, s3Service)
  val shoutout = "shoutout"

  def sendShoutout = pathPrefix(shoutout / "send") {
    val ByteJsonFormat = null
    authenticate(unverified _) {
      user =>
        post {
          formFields('image.as[Array[Byte]], 'imageText.?, 'groupIds.?, 'friendIds.?) {
            (image, imageText, groupIds, friendIds) =>
              complete {

                shoutoutService.saveImage(image).map { url =>
                  shoutoutService.sendToRecipients(user, url, imageText, groupIds, friendIds)
                }
              }
          }
        }
    }
  }

  //  def findShouts = pathPrefix(shoutout / "find") {
  //    authenticate(unverified _) { user =>
  //      {
  //        get {
  //          shoutoutService.findAllForUser(user)
  //        }
  //      }
  //    }
  //  }

  val shoutoutRoute = sendShoutout
}
