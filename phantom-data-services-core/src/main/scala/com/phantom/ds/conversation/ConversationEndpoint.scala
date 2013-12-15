package com.phantom.ds.conversation

import spray.http.MediaTypes._
import com.phantom.ds.DataHttpService
import com.phantom.model.{ ConversationItem, ConversationSummary }

import scala.Some
import spray.http.MultipartFormData

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
    } ~
      pathPrefix("api" / conversation / "startOrUpdate") {
        post {
          handleWith { data : MultipartFormData =>

            val inputTuple = for {
              imageText <- data.get("imageText") match {
                case Some(imageTextString) =>
                  imageTextString.entity.asString
                case None =>
                  ""
              }

              //              userList <- data.get("userList[]") match {
              //                case Some(userListAry) =>
              //                  userListAry.entity.asString
              //                case None =>
              //                  ""
              //              }

              userId <- data.get("userid") match {
                case Some(userIdString) =>
                  userIdString.entity.asString
                case None =>
                  ""
              }
            } yield (imageText, userId)

            println(">>>>" + inputTuple(0))
            //            println(">>>>" + inputTuple._2)
            println(">>>>" + inputTuple(1))

            data.get("image") match {
              case Some(imageEntity) =>
              //write the bytes to disk and return a url

              case None              =>
              //image wasnt provided this is an error

            }

            ConversationInsertResponse(100)
          }
        }
      }

}
