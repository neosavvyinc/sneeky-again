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

  //ONLY USED BY TESTS
  def findByConversationId(conversationId : Long) : List[ConversationItem] = {
    db.withSession { implicit session =>
      val items = Query(ConversationItemTable) filter { _.conversationId === conversationId }
      items.list()
    }
  }

  def findById(conversationItemId : Long) : ConversationItem = {
    db.withSession { implicit session =>
      val items = Query(ConversationItemTable) filter { _.id === conversationItemId }
      items.first
    }
  }

  //ONLY USED BY TESTS
  def deleteByConversationId(conversationId : Long) : Int = {
    db.withTransaction { implicit session =>
      val deleteQuery = Query(ConversationItemTable) filter { _.conversationId === conversationId }
      deleteQuery delete
    }
  }

  def updateViewed(conversationItemId : Long, userId : Long)(implicit session : Session) : Int = {
    val q = for { c <- ConversationItemTable if c.id === conversationItemId && c.toUser === userId } yield c.isViewed
    q.update(true)
  }

  def swapConversationItems(sourceUser : Long, desUser : Long)(implicit session : Session) : Int = {
    val q = for { c <- ConversationItemTable if c.toUser === sourceUser } yield c.toUser
    q.update(desUser)
  }

}

