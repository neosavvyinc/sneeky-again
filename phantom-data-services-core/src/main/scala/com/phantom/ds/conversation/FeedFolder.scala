package com.phantom.ds.conversation

import com.phantom.model._
import org.joda.time.DateTime
import com.phantom.model.Conversation
import com.phantom.model.ConversationItem
import com.phantom.model.FeedEntry
import scala.Some

object FeedFolder {

  implicit def dateTimeOrdering : Ordering[DateTime] = Ordering.fromLessThan(_ isAfter _)

  def foldFeed(userId : Long, raw : List[(Conversation, ConversationItem)], paging : Paging) : List[FeedEntry] = {
    val grouped = raw.groupBy(_._1)

    grouped.foldRight(List[FeedEntry]()) { (item, feed) =>
      toFeedEntry(userId, item._1, item._2) match {
        case None    => feed
        case Some(x) => x :: feed
      }
    }.sortBy(_.conversation.lastUpdated).paginate(paging)
  }

  private def toFeedEntry(userId : Long, conversation : Conversation, conversationItems : List[(Conversation, ConversationItem)]) : Option[FeedEntry] = {
    conversationItems.collect(filterDeleted(userId)) match {
      case Nil => None
      case x   => Some(FeedEntry(conversation, x))
    }
  }

  private def filterDeleted(userId : Long) = new PartialFunction[(Conversation, ConversationItem), ConversationItem] {

    override def isDefinedAt(x : (Conversation, ConversationItem)) : Boolean = {
      val item = x._2

      //TODO: Decide if allowing users to send to themselves is wise to support
      if (item.toUser == item.fromUser) {
        !(item.fromUserDeleted || item.toUserDeleted)
      } else {
        (item.fromUser == userId && !item.fromUserDeleted) || (item.toUser == userId && !item.toUserDeleted)
      }
    }

    override def apply(v1 : (Conversation, ConversationItem)) : ConversationItem = v1._2

  }

  //just showing off a bit here :)
  private[this] implicit class ListPaginator[T](x : List[T]) {
    def paginate(paging : Paging) : List[T] = {

      paging match {
        case NoPaging => x
        case PageRequest(page, size) => {
          val offset = (page - 1) * size
          val end = offset + size
          x.slice(offset, end)
        }
      }
    }
  }
}