package com.phantom.dataAccess

import scala.slick.session.Database
import com.phantom.model.PhantomUser
import org.joda.time.LocalDate

class PhantomUserDAO(name : String, dal : DataAccessLayer, db : Database) extends BaseDAO(name, dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def createSampleUsers = {

    implicitSession.withTransaction {
      println("in a transaction...")

      UserTable.insertAll(
        PhantomUser(None, "chris@test.com", new LocalDate(2003, 12, 21), true, "1234567"),
        PhantomUser(None, "adam@test.com", new LocalDate(2003, 12, 21), true, "1234567")
      )

      // uncomment this, the transaction will fail and no users
      // will be inserted
      // val dumbComputation : Int = 1 / 0
    }

  }
}

