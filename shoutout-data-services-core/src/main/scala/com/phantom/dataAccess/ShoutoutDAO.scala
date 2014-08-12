package com.phantom.dataAccess

import com.phantom.ds.framework.{ Dates, Logging }
import com.phantom.model.{ Friend, ShoutoutResponse, Shoutout, ShoutoutUser }

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
    u <- UserTable if s.sender === u.id
  } yield (s, u)

  def findAllForUser(user : ShoutoutUser)(implicit session : Session) : List[ShoutoutResponse] = {
    unviewedByOwnerQuery(user.id.get).list map (x => {
      val s = x._1
      val u = x._2
      ShoutoutResponse(
        s.id.get, Friend(
          u.id,
          u.username,
          u.facebookID,
          u.firstName,
          u.lastName,
          u.profilePictureUrl),
        s.text,
        s.imageUrl,
        s.createdDate
      )
    })
  }

  def setViewed(user : ShoutoutUser, id : Long)(implicit sessions : Session) : Int = {
    val q = for {
      s <- ShoutoutTable if s.recipient === user.id.get && s.id === id
    } yield s.isViewed ~ s.viewedDate

    q.update((true, Dates.nowDT))
  }

}
