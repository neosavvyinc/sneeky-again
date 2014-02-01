package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import scala.slick.session.Database
import com.phantom.ds.DSConfiguration
import scala.concurrent.ExecutionContext
import java.util.Properties
import com.jolbox.bonecp.{ BoneCPConfig, BoneCPDataSource }
import com.phantom.ds.framework.Logging

trait DatabaseSupport extends DSConfiguration with Logging {

  private implicit def executionContext : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db = {
    val dsConfig = new BoneCPConfig
    dsConfig.setPoolName("mainPool")
    dsConfig.setJdbcUrl(DBConfiguration.url)
    dsConfig.setUsername(DBConfiguration.user)
    dsConfig.setPassword(DBConfiguration.pass)
    dsConfig.setMinConnectionsPerPartition(DBConfiguration.minConnectionsPerPartition)
    dsConfig.setMaxConnectionsPerPartition(DBConfiguration.maxConnectionsPerPartition)
    dsConfig.setStatementsCacheSize(DBConfiguration.statementCacheSize)
    dsConfig.setPartitionCount(DBConfiguration.numPartitions)
    dsConfig.setPoolAvailabilityThreshold(5)

    debug(dsConfig.toString)

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

}
