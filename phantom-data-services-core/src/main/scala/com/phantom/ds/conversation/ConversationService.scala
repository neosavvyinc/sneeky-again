package com.phantom.ds.conversation

import scala.concurrent.{ Future, ExecutionContext }
import com.phantom.model.{ ConversationUpdateResponse, Conversation, ConversationInsertResponse, ConversationItem }
import scala.collection.mutable.{ Map => MMap }
import com.phantom.ds.framework.exception.PhantomException
import spray.http.StatusCodes
import com.phantom.dataAccess.DatabaseSupport
import scala.slick.session.Session

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
                        imageText : String,
                        imageUrl : String) : Future[ConversationInsertResponse]

}

class NoFeedFoundException extends Exception with PhantomException {
  val code = 102
}

object ConversationService {

  def apply()(implicit ec : ExecutionContext) = new ConversationService with DatabaseSupport {
    def findFeed(userId : Long) : Future[List[Conversation]] = {
      Future.successful(List(new Conversation(Some(1), 1, 1)))
    }

    def startConversation(fromUserId : Long,
                          toUserIds : List[Long],
                          imageText : String,
                          imageUrl : String) : Future[ConversationInsertResponse] = {

      val session : Session = db.createSession
      var count = 0

      session.withTransaction {
        val startedConversations : List[Conversation] = for (toUserId <- toUserIds) yield Conversation(None, toUserId, fromUserId)
        val conversationsFromDB : List[Conversation] = startedConversations.map {
          conversation => conversations.insert(conversation)
        }

        conversationsFromDB.foreach {
          conversation => conversationItems.insert(ConversationItem(None, conversation.id.get, imageUrl, imageText))
        }

        count = startedConversations.size
      }

      session.close()

      Future.successful(ConversationInsertResponse(count))
    }

    def respondToConversation(conversationId : Long,
                              imageText : String,
                              imageUrl : String) : Future[ConversationUpdateResponse] = {

      val session : Session = db.createSession
      session.withTransaction {

        conversationItems.insert(ConversationItem(None, conversationId, imageUrl, imageText))

        Future.successful(ConversationUpdateResponse(1))
      }

    }
  }

}