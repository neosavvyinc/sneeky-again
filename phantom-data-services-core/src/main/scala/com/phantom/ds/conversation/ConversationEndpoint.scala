package com.phantom.ds.conversation

import spray.http.MediaTypes._
import com.phantom.ds.DataHttpService
import com.phantom.model._
import com.phantom.ds.framework.httpx._

import scala._
import spray.http.MultipartFormData
import java.io.FileOutputStream
import scala.concurrent.{ Future, ExecutionContext }
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.phantom.dataAccess.DatabaseSupport
import scala.concurrent.ExecutionContext.Implicits._
import com.phantom.model.Conversation
import com.phantom.model.BlockUserByConversationResponse
import com.phantom.model.Conversation
import com.phantom.model.BlockUserByConversationResponse
import scala.Some

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:37 PM
 */
trait ConversationEndpoint extends DataHttpService {
  implicit def ex : ExecutionContext = global
  val conversationService = ConversationService()
  val conversation = "conversation"

  val conversationRoute =

    pathPrefix(conversation) {
      path(IntNumber) {
        id =>
          get {
            respondWithMediaType(`application/json`) {
              complete(
                //                conversationService.findFeed(id)
                List(
                  Conversation(Some(1), 1, 1),
                  Conversation(Some(1), 1, 1)
                )
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
            formFields('image.as[Array[Byte]], 'imageText, 'userid, 'toUsers) { (image, imageText, userid, toUsers) =>

              println("imageText> " + imageText)
              println("userid> " + userid)
              println("toUsers> " + toUsers)

              val fos : FileOutputStream = new FileOutputStream("testAdam.png")

              try {
                fos.write(image);
              } finally {
                fos.close();
              }
              complete {

                List(
                  Conversation(Some(1), 1, 1),
                  Conversation(Some(1), 1, 1)
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
            formFields('image.as[Array[Byte]], 'imageText, 'convId.as[Int]?) { (image, imageText, convId) =>

              println("imageText> " + imageText)
              println("convId> " + convId)

              //TODO Make sure this file is saved outside classpath
              //TODO Make sure this file is unique to each conversation so that we can clean it later
              val fos : FileOutputStream = new FileOutputStream("testAdam.png");
              try {
                fos.write(image);
              } finally {
                fos.close();
              }
              complete {

                List(
                  Conversation(Some(1), 1, 1),
                  Conversation(Some(1), 1, 1)
                )

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
