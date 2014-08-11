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

  val unviewedByOwnerQuery = for {
    ownerId <- Parameters[Long]
    s <- ShoutoutTable if s.recipient === ownerId && s.isViewed === false
  } yield s

  def findAllForUser(user : ShoutoutUser)(implicit session : Session) : List[Shoutout] = {
    unviewedByOwnerQuery(user.id.get).list
  }

}
