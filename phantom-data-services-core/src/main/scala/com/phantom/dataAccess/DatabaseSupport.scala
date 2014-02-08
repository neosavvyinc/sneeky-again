package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import com.phantom.ds.DSConfiguration
import scala.concurrent.ExecutionContext
import com.jolbox.bonecp.BoneCPConfig
import com.phantom.ds.framework.Logging
import com.phantom.dataAccess.PhantomDatabase._

trait DatabaseSupport extends DSConfiguration with Logging {

  private implicit def executionContext : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  // again, creating a DAL requires a Profile, which in this case is the MySQLDriver
  val dataAccessLayer = new DataAccessLayer(MySQLDriver)
  val phantomUsersDao = new PhantomUserDAO(dataAccessLayer, db)
  val conversationDao = new ConversationDAO(dataAccessLayer, db)
  val conversationItemDao = new ConversationItemDAO(dataAccessLayer, db)
  val stubUsersDao = new StubUserDAO(dataAccessLayer, db)
  val stubConversationsDao = new StubConversationDAO(dataAccessLayer, db)
  val contacts = new ContactDAO(dataAccessLayer, db)
  val sessions = new SessionDAO(dataAccessLayer, db)

  //  dataAccessLayer.create(db)

}
