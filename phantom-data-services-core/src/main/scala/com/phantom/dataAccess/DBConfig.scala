package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import scala.slick.session.{ Database, Session }
import com.phantom.ds.DSConfiguration

trait DBConfig {
  def userServiceDB : PhantomUserDAO
}

trait TestDB extends DBConfig with DSConfiguration {

  val db = Database.forURL(
    TestDBConfiguration.url,
    TestDBConfiguration.user,
    TestDBConfiguration.pass,
    null,
    TestDBConfiguration.driver)

  // creating a DAL requires a Profile, which in this case is the MySQLDriver
  val userServiceDB = new PhantomUserDAO("MySQL Dev", new DataAccessLayer(MySQLDriver), db)

}

trait ProductionDB extends DBConfig with DSConfiguration {

  val db = Database.forURL(
    DBConfiguration.url,
    DBConfiguration.user,
    DBConfiguration.pass,
    null,
    DBConfiguration.driver
  )

  // again, creating a DAL requires a Profile, which in this case is the MySQLDriver
  val userServiceDB = new PhantomUserDAO("MySQL Dev", new DataAccessLayer(MySQLDriver), db)

  //users.purgeDB
  userServiceDB.createDB
  userServiceDB.createSampleUsers
}
