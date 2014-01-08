package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import scala.slick.session.{ Database, Session }
import com.phantom.ds.DSConfiguration

trait DBConfig {
  def users : PhantomUserDAO
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
  val users = new PhantomUserDAO("MySQL Dev", new DataAccessLayer(MySQLDriver), db)

  //users.purgeDB
  users.createDB
  users.createSampleUsers
}
