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

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def insert(conversationItem : ConversationItem) : ConversationItem = {
    val id = ConversationItemTable.forInsert.insert(conversationItem)
    new ConversationItem(Some(id), conversationItem.conversationId, conversationItem.imageUrl, conversationItem.imageText)
  }

  def insertAll(conversationItems : Seq[ConversationItem]) : Future[Seq[ConversationItem]] = {
    future {
      val b = ConversationItemTable.forInsert.insertAll(conversationItems : _*)
      b.zip(conversationItems).map {
        case (id, conversationItem) =>
          conversationItem.copy(id = Some(id))
      }
    }
  }

  def findByConversationId(conversationId : Long) : List[ConversationItem] = {
    val items = Query(ConversationItemTable) filter { _.conversationId === conversationId }
    items.list()
  }

  def deleteByConversationId(conversationId : Long) : Int = {
    val deleteQuery = Query(ConversationItemTable) filter { _.conversationId === conversationId }
    deleteQuery delete
  }

}

