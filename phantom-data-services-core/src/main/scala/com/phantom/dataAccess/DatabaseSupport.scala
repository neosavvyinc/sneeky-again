package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import scala.slick.session.{ Database, Session }
import com.phantom.ds.DSConfiguration
import scala.concurrent.ExecutionContext
import java.util.Properties

trait DatabaseSupport extends DSConfiguration {

  private implicit def executionContext : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  var dbProps = new Properties()
  dbProps.setProperty("autoReconnect", "true")

  val db = Database.forURL(
    DBConfiguration.url,
    DBConfiguration.user,
    DBConfiguration.pass,
    dbProps,
    DBConfiguration.driver
  )

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
  dataAccessLayer.create(db.createSession())

}
