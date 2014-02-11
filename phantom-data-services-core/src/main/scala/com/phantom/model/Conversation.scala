package com.phantom.model

import com.phantom.dataAccess.Profile
import scala.slick.lifted.ColumnOption.DBType
import java.util.UUID

case class FeedEntry(conversation : Conversation, items : List[ConversationItem])

case class ConversationItem(id : Option[Long],
                            conversationId : Long,
                            imageUrl : String,
                            imageText : String)

case class Conversation(id : Option[Long],
                        toUser : Long,
                        fromUser : Long)

case class ConversationInsertResponse(createdCount : Long)

case class ConversationUpdateResponse(id : Long)

case class BlockUserByConversationResponse(id : Long,
                                           success : Boolean)

trait ConversationComponent { this : Profile with UserComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  object ConversationTable extends Table[Conversation]("CONVERSATIONS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def toUser = column[Long]("TO_USER")
    def fromUser = column[Long]("FROM_USER")
    def toUserFK = foreignKey("TO_USER_FK", toUser, UserTable)(_.id)
    def fromUserFK = foreignKey("FROM_USER_FK", fromUser, UserTable)(_.id)

    def * = id.? ~ toUser ~ fromUser <> (Conversation, Conversation.unapply _)
    def forInsert = * returning id

  }

}

trait ConversationItemComponent { this : Profile with ConversationComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  object ConversationItemTable extends Table[ConversationItem]("CONVERSATION_ITEMS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def conversationId = column[Long]("CONVERSATION_ID")
    def imageUrl = column[String]("IMAGE_URL")
    def imageText = column[String]("IMAGE_TEXT")
    def conversationFK = foreignKey("CONVERSATION_FK", conversationId, ConversationTable)(_.id)

    def * = id.? ~ conversationId ~ imageUrl ~ imageText <> (ConversationItem, ConversationItem.unapply _)
    def forInsert = id.? ~ conversationId ~ imageUrl ~ imageText <> (ConversationItem, ConversationItem.unapply _) returning id
  }

}