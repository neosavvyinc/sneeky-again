package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import scala.slick.session.Database
import com.phantom.ds.DSConfiguration
import scala.concurrent.ExecutionContext
import java.util.Properties
import com.jolbox.bonecp.{ BoneCPConfig, BoneCPDataSource }

trait DatabaseSupport extends DSConfiguration {

  private implicit def executionContext : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db = {
    val dsConfig = new BoneCPConfig
    dsConfig.setJdbcUrl(DBConfiguration.url)
    dsConfig.setUser(DBConfiguration.user)
    dsConfig.setPassword(DBConfiguration.pass)
    dsConfig.setMinConnectionsPerPartition(5)
    dsConfig.setMaxConnectionsPerPartition(20)
    dsConfig.setPartitionCount(1)

    val ds = new BoneCPDataSource(dsConfig)
    Database forDataSource (ds)
  }

  // again, creating a DAL requires a Profile, which in this case is the MySQLDriver
  val dataAccessLayer = new DataAccessLayer(MySQLDriver)
  val phantomUsersDao = new PhantomUserDAO(dataAccessLayer, db)
  val conversationDao = new ConversationDAO(dataAccessLayer, db)
  val conversationItemDao = new ConversationItemDAO(dataAccessLayer, db)
  val stubUsersDao = new StubUserDAO(dataAccessLayer, db)
  val stubConversationsDao = new StubConversationDAO(dataAccessLayer, db)
  val contacts = new ContactDAO(dataAccessLayer, db)
  val sessions = new SessionDAO(dataAccessLayer, db)

  //umm.....we should move this :)
  dataAccessLayer.create(db)

}
