package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import scala.slick.session.{ Database, Session }
import com.phantom.ds.DSConfiguration

trait DBConfig {
  def users : PhantomUserDAO
  def conversations : ConversationDAO
  def contacts : ContactDAO
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
  val users = new PhantomUserDAO("MySQL Dev", dataAccessLayer, db)
  val conversations = new ConversationDAO("MySQL Dev", dataAccessLayer, db)
  val conversationItems = new ConversationItemDAO("MySQL Dev", dataAccessLayer, db)
  val contacts = new ContactDAO("MySQL", dataAccessLayer, db)

  //users.purgeDB
  //dataAccessLayer.drop(db.createSession())
  dataAccessLayer.create(db.createSession())

  //TODO: Remove this - I think this is kind of test code
  users.createSampleUsers
  contacts.createSampleContacts
}
