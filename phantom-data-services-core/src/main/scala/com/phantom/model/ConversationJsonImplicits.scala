package com.phantom.model

import spray.json._

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 1:44 PM
 */
object ConversationJsonImplicits extends DefaultJsonProtocol {

  implicit val conversationStarterFormat = jsonFormat4(ConversationStarter)
  implicit val conversationItemFormat = jsonFormat6(ConversationItem)
  implicit val conversationSummaryFormat = jsonFormat1(ConversationSummary)
  implicit val conversationDetail = jsonFormat2(ConversationDetail)
  implicit val conversationFeed = jsonFormat1(Feed)

}
