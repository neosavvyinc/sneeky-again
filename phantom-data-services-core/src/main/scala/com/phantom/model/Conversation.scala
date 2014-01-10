package com.phantom.model

import com.phantom.dataAccess.Profile
import scala.slick.lifted.ColumnOption.DBType

case class ConversationStarter(
  toUsers : List[Long],
  fromUser : Long,
  imageUrl : String,
  imageText : String)

case class ConversationItem(id : Option[Long],
                            conversationId : Long,
                            imageUrl : String,
                            imageText : String)

case class Conversation(id : Option[Long],
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
  import com.github.tototoshi.slick.JodaSupport._

  object ConversationTable extends Table[Conversation]("CONVERSATIONS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def toUser = column[Long]("TO_USER")
    def fromUser = column[Long]("FROM_USER")

    def * = id.? ~ toUser ~ fromUser <> (Conversation, Conversation.unapply _)
    def forInsert = id.? ~ toUser ~ fromUser <> (Conversation, Conversation.unapply _) returning id

  }

}

trait ConversationItemComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  object ConversationItemTable extends Table[ConversationItem]("CONVERSATION_ITEMS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def conversationId = column[Long]("CONVERSATION_ID")
    def imageUrl = column[String]("IMAGE_URL")
    def imageText = column[String]("IMAGE_TEXT")

    def * = id.? ~ conversationId ~ imageUrl ~ imageText <> (ConversationItem, ConversationItem.unapply _)
    def forInsert = id.? ~ conversationId ~ imageUrl ~ imageText <> (ConversationItem, ConversationItem.unapply _) returning id
  }

}