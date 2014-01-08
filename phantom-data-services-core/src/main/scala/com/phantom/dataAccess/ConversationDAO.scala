package com.phantom.dataAccess

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/7/14
 * Time: 9:35 PM
 * To change this template use File | Settings | File Templates.
 */

import scala.slick.session.Database

class ConversationDAO(name : String, dal : DataAccessLayer, db : Database) extends BaseDAO(name, dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

}

