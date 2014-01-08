package com.phantom.model

import com.phantom.dataAccess.Profile

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

case class Conversation(
  id : Long,
  toUser : Long,
  fromUser : Long)

case class ConversationSummary(mostRecent : ConversationItem)

case class ConversationDetail(id : Long,
                              conversationItems : List[ConversationItem])

case class Feed(conversations : List[ConversationSummary])

case class ConversationInsertResponse(id : Long)

case class BlockUserByConversationResponse(id : Long)

trait ConversationComponent { this : Profile =>

  import profile.simple._

  //  object ConversationTable extends Table[Conversation]

}
