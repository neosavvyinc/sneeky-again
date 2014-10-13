package com.sneeky.ds.sneeky

import java.util.UUID

import com.sneeky.ds.framework.exception.ShoutoutException
import spray.http.MediaTypes._
import com.sneeky.ds.{ BasicCrypto, DataHttpService }
import com.sneeky.ds.framework.auth.RequestAuthenticator
import akka.actor.{ ActorRefFactory, ActorRef }
import com.sneeky.model.{ SneekyV2User, UserStatus, Paging }
import com.sneeky.ds.integration.amazon.S3Service
import spray.http.StatusCodes
import spray.routing.RequestContext
import spray.routing.authentication.Authentication

import scala.concurrent.{ ExecutionContext, Future, future }

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

  def like = pathPrefix(sneeky / "like" / IntNumber) {
    sneekyId =>

      authenticate(unverified _) {
        authenticationResult =>
          val (user, sessionId) = authenticationResult

          post {
            respondWithMediaType(`application/json`) {
              complete {
                future {
                  sneekyService.like(user, sessionId, sneekyId)
                }
              }
            }
          }
      }

  }

  def unlike = pathPrefix(sneeky / "like" / IntNumber) {
    sneekyId =>

      authenticate(unverified _) {
        authenticationResult =>
          val (user, sessionId) = authenticationResult

          delete {
            respondWithMediaType(`application/json`) {
              complete {
                future {
                  sneekyService.unlike(user, sessionId, sneekyId)
                }
              }
            }
          }
      }
  }

  def dislike = pathPrefix(sneeky / "dislike" / IntNumber) {
    sneekyId =>
      authenticate(unverified _) {
        authenticationResult =>
          val (user, sessionId) = authenticationResult

          post {
            respondWithMediaType(`application/json`) {
              complete {
                future {
                  sneekyService.dislike(user, sessionId, sneekyId)
                }
              }
            }
          }
      }

  }

  def undislike = pathPrefix(sneeky / "dislike" / IntNumber) {
    sneekyId =>

      authenticate(unverified _) {
        authenticationResult =>
          val (user, sessionId) = authenticationResult

          delete {
            respondWithMediaType(`application/json`) {
              complete {
                future {
                  sneekyService.undislike(user, sessionId, sneekyId)
                }
              }
            }
          }
      }
  }

  val DEFAULT_PAGESIZE = 10;
  val DEFAULT_PAGENUMBER = 0;

  def feedByDate = pathPrefix(sneeky / "feedByDate") {
    parameters('pageSize.as[Option[Int]], 'pageNumber.as[Option[Int]]) {
      (pageSize, pageNumber) =>

        authenticate(unverified _) {
          authenticationResult =>
            val (user, sessionId) = authenticationResult
            val actualPageSize = Math.abs(pageSize.getOrElse(DEFAULT_PAGESIZE))
            val actualPageNumber = Math.abs(pageNumber.getOrElse(DEFAULT_PAGENUMBER))

            complete {
              future {
                sneekyService.findFeedByDateForUser(user, sessionId, actualPageSize, actualPageNumber)
              }
            }
        }
    }
  }

  /**
   * Sorted by count of likes + dislikes
   * @return
   */
  def feedByPopularity = pathPrefix(sneeky / "feedByPopularity") {
    parameters('pageSize.as[Option[Int]], 'pageNumber.as[Option[Int]]) {
      (pageSize, pageNumber) =>

        authenticate(unverified _) {
          authenticationResult =>
            val (user, sessionId) = authenticationResult
            val actualPageSize = Math.abs(pageSize.getOrElse(DEFAULT_PAGESIZE))
            val actualPageNumber = Math.abs(pageNumber.getOrElse(DEFAULT_PAGENUMBER))

            complete {
              future {
                sneekyService.findFeedByPopularity(user, sessionId, actualPageSize, actualPageNumber)
              }
            }
        }
    }
  }

  def myFeed = pathPrefix(sneeky / "myFeed") {
    parameters('pageSize.as[Option[Int]], 'pageNumber.as[Option[Int]]) {
      (pageSize, pageNumber) =>

        authenticate(unverified _) {
          authenticationResult =>
            val (user, sessionId) = authenticationResult
            val actualPageSize = Math.abs(pageSize.getOrElse(DEFAULT_PAGESIZE))
            val actualPageNumber = Math.abs(pageNumber.getOrElse(DEFAULT_PAGENUMBER))

            complete {
              future {
                sneekyService.findMyFeed(user, sessionId, actualPageSize, actualPageNumber)
              }
            }
        }
    }
  }

  val shoutoutRoute =
    sendSneek ~
      like ~
      unlike ~
      dislike ~
      undislike ~
      feedByDate ~
      feedByPopularity ~
      myFeed

}
