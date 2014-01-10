package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import scala.slick.session.{ Database, Session }
import com.phantom.ds.DSConfiguration

trait DBConfig {
  def userServiceDB : PhantomUserDAO
  def conversations : ConversationDAO
}

trait DatabaseSupport extends DBConfig with DSConfiguration {

  val db = Database.forURL(
    DBConfiguration.url,
    DBConfiguration.user,
    DBConfiguration.pass,
    null,
    DBConfiguration.driver
  )

  // again, creating a DAL requires a Profile, which in this case is the MySQLDriver
  val dataAccessLayer = new DataAccessLayer(MySQLDriver);
  val userServiceDB = new PhantomUserDAO("MySQL Dev", dataAccessLayer, db)
  val conversations = new ConversationDAO("MySQL Dev", dataAccessLayer, db)
  val conversationItems = new ConversationItemDAO("MySQL Dev", dataAccessLayer, db)

  //users.purgeDB
  dataAccessLayer.create(db.createSession())

  //TODO: Remove this - I think this is kind of test code
  userServiceDB.createSampleUsers
}
