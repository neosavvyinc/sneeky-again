package com.phantom.ds.conversation

import scala.concurrent.{ Future, ExecutionContext }
import com.phantom.model.Feed
import scala.collection.mutable.{ Map => MMap }
import com.phantom.ds.framework.exception.PhantomException

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
  def findFeed(userId : Long) : Future[Feed]

}

class NoFeedFoundException extends Exception with PhantomException {
  val code = 102
}

object ConversationService {

  def apply()(implicit ec : ExecutionContext) = MConversationService

}

object MConversationService extends ConversationService {

  val feedMap : MMap[Long, Feed] = MMap.empty

  def findFeed(userId : Long) : Future[Feed] = {
    feedMap.get(userId) match {
      case Some(x) => Future.successful { x }
      case None    => Future.failed(new NoFeedFoundException())
    }
  }

}
