package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import scala.slick.session.{ Database, Session }
import com.phantom.ds.DSConfiguration

trait DatabaseSupport extends DSConfiguration {

  val db = Database.forURL(
    DBConfiguration.url,
    DBConfiguration.user,
    DBConfiguration.pass,
    null,
    DBConfiguration.driver
  )

  // again, creating a DAL requires a Profile, which in this case is the MySQLDriver
  val dataAccessLayer = new DataAccessLayer(MySQLDriver);
  val phantomUsers = new PhantomUserDAO(dataAccessLayer, db)
  val conversations = new ConversationDAO(dataAccessLayer, db)
  val conversationItems = new ConversationItemDAO(dataAccessLayer, db)
  val contacts = new ContactDAO(dataAccessLayer, db)

  //users.purgeDB
  //dataAccessLayer.drop(db.createSession())
  dataAccessLayer.create(db.createSession())

  //TODO: Remove this - I think this is kind of test code
  phantomUsers.createSampleUsers
  contacts.createSampleContacts
}
