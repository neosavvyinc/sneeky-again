package com.phantom.dataAccess

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/7/14
 * Time: 9:35 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.session.Database
import com.phantom.model.Conversation

class ConversationDAO(name : String, dal : DataAccessLayer, db : Database) extends BaseDAO(name, dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def insert = { conversationItem : Conversation =>
    ConversationTable.insert(conversationItem)
  }
  def deleteById = { convId : Long =>
    val dbid = ConversationTable filter { _.id === convId }
    println(dbid.selectStatement)
    dbid delete
  }
  def findByOwnerId = { ownerId : Long =>
    Query(ConversationTable).list()
  }
  def findById = ???

  def update = ???

}

