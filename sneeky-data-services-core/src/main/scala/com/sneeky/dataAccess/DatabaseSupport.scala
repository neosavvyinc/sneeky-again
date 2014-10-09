package com.sneeky.dataAccess

import scala.slick.driver.MySQLDriver
import com.sneeky.ds.DSConfiguration
import scala.concurrent.ExecutionContext
import com.sneeky.ds.framework.Logging

trait DatabaseSupport extends DSConfiguration with Logging {

  private implicit def executionContext : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db = SneekyV2Database.db

  // again, creating a DAL requires a Profile, which in this case is the MySQLDriver
  val dataAccessLayer = new DataAccessLayer(MySQLDriver)

  val shoutoutUsersDao = new SneekyUserDAO(dataAccessLayer, db)
  val shoutoutDao = new SneekyDAO(dataAccessLayer, db)
  val sessionsDao = new SessionDAO(dataAccessLayer, db)

}
