package com.phantom.dataAccess

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/7/14
 * Time: 9:35 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.session.Database
import com.phantom.model.ConversationItem
import scala.concurrent.{ Future, ExecutionContext, future }

class ConversationItemDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

  def insertOperation(conversationItem : ConversationItem)(implicit session : Session) : ConversationItem = {
    val id = ConversationItemTable.forInsert.insert(conversationItem)
    conversationItem.copy(id = Some(id))
  }

  //ONLY USED BY TESTS
  def insertAll(conversationItems : Seq[ConversationItem]) : Future[Seq[ConversationItem]] = {
    future {
      db.withTransaction { implicit session =>
        insertAllOperation(conversationItems)
      }
    }
  }

  def insertAllOperation(conversationItems : Seq[ConversationItem])(implicit session : Session) : Seq[ConversationItem] = {
    val b = ConversationItemTable.forInsert.insertAll(conversationItems : _*)
    b.zip(conversationItems).map {
      case (id, conversationItem) =>
        conversationItem.copy(id = Some(id))
    }
  }

  private val findByConversationIdAndUserId = for {
    (conversationId, userId) <- Parameters[(Long, Long)]
    c <- ConversationItemTable if c.conversationId === conversationId && (c.fromUser === userId || c.toUser === userId)
  } yield c

  private val findByIdAndUserId = for {
    (itemId, userId) <- Parameters[(Long, Long)]
    c <- ConversationItemTable if c.id === itemId && (c.fromUser === userId || c.toUser === userId)
  } yield c

  def findByConversationIdAndUserOperation(conversationId : Long, userId : Long)(implicit session : Session) : List[ConversationItem] = {
    findByConversationIdAndUserId(conversationId, userId).list
  }

  def findByIdAndUserOperation(itemId : Long, userId : Long)(implicit session : Session) : Option[ConversationItem] = {
    findByIdAndUserId(itemId, userId).firstOption
  }

  def updateViewedOperation(conversationItemId : Long, userId : Long)(implicit session : Session) : Int = {
    val q = for { c <- ConversationItemTable if c.id === conversationItemId && c.toUser === userId } yield c.isViewed
    q.update(true)
  }

  def swapConversationItemsOperation(sourceUser : Long, desUser : Long)(implicit session : Session) : Int = {
    val q = for { c <- ConversationItemTable if c.toUser === sourceUser } yield c.toUser
    q.update(desUser)
  }

  def updateDeletedByToUserOperation(ids : Long*)(implicit session : Session) : Int = {
    if (ids.size > 0) {
      val q = for { c <- ConversationItemTable if c.id inSet ids } yield c.toUserDeleted
      q.update(true)
    } else {
      0
    }
  }

  def updateDeletedByFromUserOperation(ids : Long*)(implicit session : Session) : Int = {
    if (ids.size > 0) {
      val q = for { c <- ConversationItemTable if c.id inSet ids } yield c.fromUserDeleted
      q.update(true)
    } else {
      0
    }
  }

}

