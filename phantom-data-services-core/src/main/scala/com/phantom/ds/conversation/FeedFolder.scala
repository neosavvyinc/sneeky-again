package com.phantom.ds.conversation

import com.phantom.model.{ FeedEntry, Conversation, ConversationItem }

object FeedFolder {

  def foldFeed(userId : Long, raw : List[(Conversation, ConversationItem)]) : List[FeedEntry] = {
    val grouped = raw.groupBy(_._1)
    grouped.foldRight(List[FeedEntry]()) { (item, feed) =>
      toFeedEntry(userId, item._1, item._2) match {
        case None    => feed
        case Some(x) => x :: feed
      }
    }
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
      (item.fromUser == userId && !item.fromUserDeleted) || (item.toUser == userId && !item.toUserDeleted)
    }

    override def apply(v1 : (Conversation, ConversationItem)) : ConversationItem = v1._2

  }

}