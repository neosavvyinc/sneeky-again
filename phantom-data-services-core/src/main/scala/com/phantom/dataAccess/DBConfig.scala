package com.phantom.dataAccess

import scala.slick.driver.MySQLDriver
import scala.slick.session.{ Database, Session }

trait DBConfig {
  def users : PhantomUserDAO
}

trait TestDB extends DBConfig {
  // We could pull this out into a properties/conf file like Nick did
  val db = Database.forURL(
    "jdbc:mysql://localhost/phantom_test?user=root",
    driver = "com.mysql.jdbc.Driver")

  // creating a DAL requires a Profile, which in this case is the MySQLDriver
  val users = new PhantomUserDAO("MySQL Dev", new DataAccessLayer(MySQLDriver), db)

}

trait ProductionDB extends DBConfig {

  // We could pull this out into a properties/conf file like Nick did
  val db = Database.forURL(
    "jdbc:mysql://localhost/phantom?user=root",
    driver = "com.mysql.jdbc.Driver")

  // again, creating a DAL requires a Profile, which in this case is the MySQLDriver
  val users = new PhantomUserDAO("MySQL Dev", new DataAccessLayer(MySQLDriver), db)

  //users.purgeDB
  users.createDB
  //users.createSampleUsers
}
