package com.phantom.ds.conversation

import spray.http.MediaTypes._
import com.phantom.ds.DataHttpService
import com.phantom.model.{ ConversationItem, ConversationSummary }

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

  case class ConversationInsertResponse(id : Long)

  implicit val conversationResponse = jsonFormat1(ConversationInsertResponse)

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
        path("startOrUpdate") {
          post {
            formFields('image.as[Array[Byte]], 'imageText, 'userid, 'toUsers, 'convId.as[Int]?) { (image, imageText, userid, toUsers, convId) =>

              println("imageText> " + imageText)
              println("userid> " + userid)
              println("toUsers> " + toUsers)
              println("convId> " + convId)

              val fos : FileOutputStream = new FileOutputStream("testAdam.png");
              try {
                fos.write(image);
              } finally {
                fos.close();
              }
              complete {
                "0"
              }
            }
          }
        }
      }
    } ~ {
      val ByteJsonFormat = null

      import spray.httpx.encoding.{ NoEncoding, Gzip }

      pathPrefix(conversation) {
        path("upload") {
          post {
            formField('imageupload.as[Array[Byte]]) { file =>
              val fos : FileOutputStream = new FileOutputStream("test.png");
              try {
                fos.write(file);
              } finally {
                fos.close();
              }
              complete {
                "0"
              }
            }
          }
        }
      }
    }

}
