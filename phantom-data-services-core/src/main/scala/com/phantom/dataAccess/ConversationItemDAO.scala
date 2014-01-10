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

class ConversationItemDAO(name : String, dal : DataAccessLayer, db : Database) extends BaseDAO(name, dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def insert(conversationItem : ConversationItem) : ConversationItem = {
    val id = ConversationItemTable.forInsert.insert(conversationItem)
    new ConversationItem(Some(id), conversationItem.conversationId, conversationItem.imageUrl, conversationItem.imageText)
  }

  def insertAll(conversationItems : List[ConversationItem]) : Unit = {
    conversationItems.foreach(insert(_))
  }

  def findByConversationId(conversationId : Long) : List[ConversationItem] = {
    val items = Query(ConversationItemTable) filter { _.conversationId === conversationId }
    items.list()
  }

  def deleteByConversationId(conversationId : Long) : Int = {
    val deleteQuery = Query(ConversationItemTable) filter { _.conversationId === conversationId }
    deleteQuery delete
  }
  //  def findById(ConversationItemId : Long) : ConversationItem = {
  //    val items = Query(ConversationItemTable) filter { _.id === ConversationItemId }
  //    items.first
  //  }
  //  def updateById(ConversationItem : ConversationItem) : Int = {
  //    val updateQuery = Query(ConversationItemTable) filter { _.id === ConversationItem.id }
  //    updateQuery.update(ConversationItem)
  //  }

}

