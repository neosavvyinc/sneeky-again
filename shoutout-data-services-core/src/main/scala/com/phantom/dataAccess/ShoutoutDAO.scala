package com.phantom.dataAccess

import com.phantom.ds.framework.Logging
import com.phantom.model.{ Shoutout, ShoutoutUser }

import scala.concurrent.ExecutionContext
import scala.slick.session.Database

/**
 * Created by aparrish on 8/11/14.
 */
class ShoutoutDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  def insertShoutouts(owner : ShoutoutUser, recipients : List[ShoutoutUser], shoutout : Shoutout)(implicit session : Session) = {
    recipients.map(rec => ShoutoutTable.forInsert.insert(shoutout.copy(recipient = rec.id.get)))
  }

  def findAllForUser(user : ShoutoutUser) : List[Shoutout] = ???

}
