package com.phantom.ds.conversation

import scala.concurrent.ExecutionContext

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
  def findFeed = ???

  def startOrUpdateConversation = ???

}

object ConversationService {

  def apply()(implicit ec : ExecutionContext) = new ConversationService {

  }

}