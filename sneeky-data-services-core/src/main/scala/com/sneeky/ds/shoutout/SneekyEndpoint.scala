package com.sneeky.ds.shoutout

import com.sneeky.ds.framework.exception.ShoutoutException
import spray.http.MediaTypes._
import com.sneeky.ds.{ BasicCrypto, DataHttpService }
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
trait SneekyEndpoint extends DataHttpService with BasicCrypto {
  this : RequestAuthenticator =>

  def appleActor : ActorRef
  def s3Service : S3Service

  val sneekyService = SneekService(appleActor, s3Service)
  val sneeky = "sneeky"

  def sendSneek = pathPrefix(sneeky / "send") {
    val ByteJsonFormat = null
    authenticate(unverified _) {
      authenticationResult =>

        val (user, sessionId) = authenticationResult

        post {
          respondWithMediaType(`application/json`) {
            formFields('data.as[Option[Array[Byte]]], 'text.?, 'contentType) {
              (data, text, contentType) =>
                complete {

                  contentType match {

                    case x if x == "video/quicktime" => sneekyService.saveData(data, "video/quicktime").map { url =>
                      sneekyService.sendToRecipients(user, url, text, contentType)
                    }
                    case x if x == "video/mp4" => sneekyService.saveData(data, "video/mp4").map { url =>
                      sneekyService.sendToRecipients(user, url, text, contentType)
                    }
                    case x if x == "audio/mp4" => sneekyService.saveData(data, "audio/mp4").map { url =>
                      sneekyService.sendToRecipients(user, url, text, contentType)
                    }
                    case x if x == "image/jpg" => sneekyService.saveData(data, "image/jpg").map { url =>
                      sneekyService.sendToRecipients(user, url, text, contentType)
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

  val shoutoutRoute =
    sendSneek
}
