package com.phantom.dataAccess

import scala.slick.session.Database

class PhantomUserDAO(name : String, dal : DAL, db : Database) extends BaseDAO(name, dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

}

