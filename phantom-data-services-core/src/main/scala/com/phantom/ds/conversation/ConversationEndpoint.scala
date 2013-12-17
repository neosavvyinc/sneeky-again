package com.phantom.ds.conversation

import spray.http.MediaTypes._
import com.phantom.ds.DataHttpService
import com.phantom.model.{ BlockUserByConversationResponse, Feed, ConversationItem, ConversationSummary }

import scala.Some
import spray.http.MultipartFormData
import java.io.FileOutputStream

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
                Feed(
                  List(
                    ConversationSummary(
                      ConversationItem(1, 1, 1L, 2L, imageText, "/path/to/image")
                    ),
                    ConversationSummary(
                      ConversationItem(1, 1, 1L, 2L, imageText, "/path/to/image")
                    )
                  )
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
                Feed(
                  List(
                    ConversationSummary(
                      ConversationItem(1, 1, 1L, 2L, imageText, "/path/to/image")
                    ),
                    ConversationSummary(
                      ConversationItem(1, 1, 1L, 2L, imageText, "/path/to/image")
                    )
                  )
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
                  BlockUserByConversationResponse(conversationService.blockUserByConversationId(id), "test")
                }
              }
            }
        }
      }

    }

}
