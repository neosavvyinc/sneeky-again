package com.shoutout.dataAccess

import com.shoutout.ds.framework.{ Dates, Logging }
import com.shoutout.model.{ Friend, ShoutoutResponse, Shoutout, ShoutoutUser }

import scala.concurrent.{ ExecutionContext, Future, future }
import scala.slick.session.Database

/**
 * Created by aparrish on 8/11/14.
 */
class ShoutoutDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  def insertShoutouts(recipients : List[ShoutoutUser], shoutout : Shoutout)(implicit session : Session) = {
    recipients.map(rec => ShoutoutTable.forInsert.insert(shoutout.copy(recipient = rec.id.get)))
  }

  val unviewedByOwnerQuery = for {
    ownerId <- Parameters[Long]
    s <- ShoutoutTable if s.recipient === ownerId && s.isViewed === false && s.isBlocked === false
    u <- UserTable if s.sender === u.id
  } yield (s, u)

  val unreadMessageQuery = for {
    ownerId <- Parameters[Long]
    s <- ShoutoutTable if (s.recipient === ownerId && s.isViewed === false)
  } yield s

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
        s.createdDate,
        s.contentType
      )
    })
  }

  def setViewed(user : ShoutoutUser, id : Long)(implicit sessions : Session) : Int = {
    val q = for {
      s <- ShoutoutTable if s.recipient === user.id.get && s.id === id
    } yield s.isViewed ~ s.viewedDate

    q.update((true, Dates.nowDT))
  }

  def countSent(user : ShoutoutUser)(implicit session : Session) : Int = {
    val q1 = Query(ShoutoutTable).filter(_.sender is user.id.get).length
    q1.run
  }

  def countReceived(user : ShoutoutUser)(implicit session : Session) : Int = {
    val q1 = Query(ShoutoutTable).filter(_.recipient is user.id.get).length
    q1.run
  }

  def countUnread(user : ShoutoutUser) : Int = {
    db.withSession { implicit session : Session =>
      import scala.slick.jdbc.{ StaticQuery => Q }
      import Q.interpolation
      val id = user.id.get
      val userName = user.username

      def countQuery = sql"select count(*) from SHOUTOUTS where RECIPIENT_ID = $id and IS_VIEWED = false and IS_BLOCKED = false".as[Int]
      try {
        val result = countQuery.first
        debug(s"Executing a count query for user: $userName and the result was $result")
        if (result == 0) { 1 } else { result }
      } catch {
        case e : Exception => {
          log.error(s"Something went bad wrong while counting a user's shoutouts: $e")
          1 //something happened so let's at least return one
        }
      }

    }

  }

}
