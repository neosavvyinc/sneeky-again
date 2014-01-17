package com.phantom.dataAccess

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/7/14
 * Time: 9:35 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.session.Database
import com.phantom.model.{ ConversationItem, Conversation }

class ConversationDAO(dal : DataAccessLayer, db : Database) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def insert(conversationItem : Conversation) : Conversation = {
    val id = ConversationTable.forInsert.insert(conversationItem)
    Conversation(Some(id), conversationItem.toUser, conversationItem.fromUser)
  }
  def findByFromUserId(fromUserId : Long) : List[Conversation] = {
    val items = Query(ConversationTable) filter { _.fromUser === fromUserId }
    items.list()
  }
  def deleteById(conversationId : Long) : Int = {
    val deleteQuery = Query(ConversationTable) filter { _.id === conversationId }
    deleteQuery delete
  }
  def findById(conversationId : Long) : Conversation = {
    val items = Query(ConversationTable) filter { _.id === conversationId }
    items.first
  }
  def updateById(conversation : Conversation) : Int = {
    val updateQuery = Query(ConversationTable) filter { _.id === conversation.id }
    updateQuery.update(conversation)
  }
  def findConversationsAndItems(fromUserId : Long) : List[(Conversation, List[ConversationItem])] = {
    val q = for {
      c <- ConversationTable
      ci <- ConversationItemTable if c.id === ci.conversationId
    } yield (c, ci)

    val results : List[(Conversation, ConversationItem)] = q.list

    var previous : Conversation = null
    var current : Conversation = null
    var combinedCollection : List[ConversationItem] = null
    var returnValue : List[(Conversation, List[ConversationItem])] = null

    //TODO: This is a total hack - we need a better way to do this
    results.foreach {
      result =>
        val (conversation, conversationItem) = result
        current = conversation
        if (previous == null || combinedCollection == null) {
          combinedCollection = (conversationItem :: Nil)
        } else if (previous == current) {
          combinedCollection = List.concat(combinedCollection, (conversationItem :: Nil))
        } else {
          if (returnValue == null) {
            returnValue = (current, combinedCollection) :: Nil
          } else {
            returnValue = List.concat(returnValue,
              ((current, combinedCollection) :: Nil))
          }
          combinedCollection = null
          combinedCollection = (conversationItem :: Nil)

        }

        previous = current
    }

    if (returnValue == null) {
      returnValue = (current, combinedCollection) :: Nil
    } else {
      returnValue = List.concat(returnValue,
        ((current, combinedCollection) :: Nil))
    }

    returnValue
  }

}

