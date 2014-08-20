package com.shoutout.model

import com.shoutout.dataAccess.Profile
import scala.slick.lifted.ColumnOption.DBType
import java.util.UUID
import org.joda.time.DateTime
import com.shoutout.ds.framework.Dates

case class FeedEntry(conversation : Conversation, items : List[ConversationItem])

case class FeedWrapper(conversation : FEConversation, items : List[FEConversationItem])

case class FEConversationItem(id : Long,
                              conversationId : Long,
                              imageUrl : String,
                              imageText : String,
                              isViewed : Boolean,
                              createdDate : DateTime,
                              isFromSender : Boolean)

case class ConversationItem(id : Option[Long],
                            conversationId : Long,
                            imageUrl : String,
                            imageText : String,
                            toUser : Long,
                            fromUser : Long,
                            isViewed : Boolean = false,
                            createdDate : DateTime = Dates.nowDT,
                            toUserDeleted : Boolean = false,
                            fromUserDeleted : Boolean = false)

case class FEConversation(id : Long,
                          receiverPhoneNumber : String,
                          lastUpdated : DateTime,
                          messagesInConversation : Int)

case class Conversation(id : Option[Long],
                        toUser : Long,
                        fromUser : Long,
                        receiverPhoneNumber : String,
                        lastUpdated : DateTime = Dates.nowDT)

case class ConversationStartRequest(toUsers : Seq[String], imageText : String, imageId : Long)

case class ConversationRespondRequest(convId : Long, imageText : String, imageId : Long)

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
    def receiverPhoneNumber = column[String]("RECV_PHONE_NUMBER")
    def lastUpdated = column[DateTime]("LAST_UPDATE_DATE", DBType("TIMESTAMP(3)"))
    def toUserFK = foreignKey("TO_USER_FK", toUser, UserTable)(_.id)
    def fromUserFK = foreignKey("FROM_USER_FK", fromUser, UserTable)(_.id)

    def * = id.? ~ toUser ~ fromUser ~ receiverPhoneNumber ~ lastUpdated <> (Conversation, Conversation.unapply _)
    def forInsert = * returning id

  }

}

trait ConversationItemComponent { this : Profile with ConversationComponent with UserComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  object ConversationItemTable extends Table[ConversationItem]("CONVERSATION_ITEMS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def conversationId = column[Long]("CONVERSATION_ID")
    def imageUrl = column[String]("IMAGE_URL")
    def imageText = column[String]("IMAGE_TEXT")
    def toUser = column[Long]("TO_USER")
    def fromUser = column[Long]("FROM_USER")
    def isViewed = column[Boolean]("IS_VIEWED", O.Default(false))
    def createdDate = column[DateTime]("CREATED_DATE", DBType("DATETIME"))
    def toUserDeleted = column[Boolean]("TO_USER_DELETE", O.Default(false))
    def fromUserDeleted = column[Boolean]("FROM_USER_DELETE", O.Default(false))

    def toUserConvFK = foreignKey("TO_CONV_USER_FK", toUser, UserTable)(_.id)
    def fromUserConvFK = foreignKey("FROM_CONV_USER_FK", fromUser, UserTable)(_.id)
    def conversationFK = foreignKey("CONVERSATION_FK", conversationId, ConversationTable)(_.id)

    def * = id.? ~ conversationId ~ imageUrl ~ imageText ~ toUser ~ fromUser ~ isViewed ~ createdDate ~ toUserDeleted ~ fromUserDeleted <> (ConversationItem, ConversationItem.unapply _)
    def forInsert = id.? ~ conversationId ~ imageUrl ~ imageText ~ toUser ~ fromUser ~ isViewed ~ createdDate ~ toUserDeleted ~ fromUserDeleted <> (ConversationItem, ConversationItem.unapply _) returning id
  }

}
