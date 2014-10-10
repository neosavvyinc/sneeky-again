package com.sneeky.dataAccess

import com.sneeky.ds.framework.{ Dates, Logging }
import com.sneeky.model.{ SneekResponse, Sneek, SneekyV2User }

import scala.concurrent.{ ExecutionContext, Future, future }
import scala.slick.session.Database

/**
 * Created by aparrish on 8/11/14.
 */
class SneekyDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  def insertSneek(shoutout : Sneek)(implicit session : Session) = {
    SneekyTable.forInsert.insert(shoutout)
  }

}
