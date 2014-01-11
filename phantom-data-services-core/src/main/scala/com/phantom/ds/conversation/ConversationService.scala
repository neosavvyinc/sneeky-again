package com.phantom.ds.conversation

import scala.concurrent.{ Future, ExecutionContext }
import com.phantom.model.{ Conversation, ConversationInsertResponse, ConversationItem }
import scala.collection.mutable.{ Map => MMap }
import com.phantom.ds.framework.exception.PhantomException
import spray.http.StatusCodes

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:01 PM
 */
trait ConversationService {

  //  * Get my feed
  //    * Request
  //    * UserId
  //    * Response
  //    * List of conversations
  def findFeed(userId : Long) : Future[List[Conversation]]

  def startConversation(fromUserId : Long,
                        toUserIds : List[Long],
                        conversationItem : ConversationItem) : Future[ConversationInsertResponse]

}

class NoFeedFoundException extends Exception with PhantomException {
  val code = 102
}

object ConversationService {

  def apply()(implicit ec : ExecutionContext) = new ConversationService {
    def findFeed(userId : Long) : Future[List[Conversation]] = {
      Future.successful(List(new Conversation(Some(1), 1, 1)))
    }

    def startConversation(fromUserId : Long,
                          toUserIds : List[Long],
                          conversationItem : ConversationItem) : Future[ConversationInsertResponse] = {
      Future.successful(new ConversationInsertResponse(1))
    }
  }

}