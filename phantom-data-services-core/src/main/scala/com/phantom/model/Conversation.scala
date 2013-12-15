package com.phantom.model

case class ConversationStarter(
  toUsers : List[Long],
  fromUser : Long,
  imageUrl : String,
  imageText : String)

case class ConversationItem(id : Long,
                            conversationId : Long,
                            toUser : Long,
                            fromUser : Long,
                            imageUrl : String,
                            imageText : String)

case class ConversationSummary(mostRecent : ConversationItem)

case class ConversationDetail(id : Long,
                              conversationItems : List[ConversationItem])

case class Feed(conversations : List[ConversationSummary])

case class ConversationInsertResponse(id : Long)