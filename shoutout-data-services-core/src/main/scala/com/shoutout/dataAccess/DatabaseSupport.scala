package com.shoutout.dataAccess

import scala.slick.driver.MySQLDriver
import com.shoutout.ds.DSConfiguration
import scala.concurrent.ExecutionContext
import com.shoutout.ds.framework.Logging

trait DatabaseSupport extends DSConfiguration with Logging {

  private implicit def executionContext : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db = ShoutoutDatabase.db

  // again, creating a DAL requires a Profile, which in this case is the MySQLDriver
  val dataAccessLayer = new DataAccessLayer(MySQLDriver)

  val shoutoutUsersDao = new ShoutoutUserDAO(dataAccessLayer, db)
  val shoutoutDao = new ShoutoutDAO(dataAccessLayer, db)
  val blockUserDao = new BlockUserDAO(dataAccessLayer, db)
  val contactsDao = new ContactDAO(dataAccessLayer, db)
  val sessionsDao = new SessionDAO(dataAccessLayer, db)
  val groupDao = new GroupDAO(dataAccessLayer, db)
  val statsDao = new StatsDAO(dataAccessLayer, db)

  //  val conversationDao = new ConversationDAO(dataAccessLayer, db)
  //  val conversationItemDao = new ConversationItemDAO(dataAccessLayer, db)

}
