package com.sneeky.dataAccess

import com.sneeky.ds.framework.{ Dates, Logging }
import com.sneeky.model.{ ShoutoutResponse, Shoutout, SneekyV2User }

import scala.concurrent.{ ExecutionContext, Future, future }
import scala.slick.session.Database

/**
 * Created by aparrish on 8/11/14.
 */
class SneekyDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  def insertSneek(shoutout : Shoutout)(implicit session : Session) = {
    ShoutoutTable.forInsert.insert(shoutout)
  }

  val unviewedByOwnerQuery = for {
    ownerId <- Parameters[Long]
    s <- ShoutoutTable if s.recipient === ownerId && s.isViewed === false && s.isBlocked === false && s.isCleaned === false
    u <- UserTable if s.sender === u.id
  } yield (s, u)

  val unreadMessageQuery = for {
    ownerId <- Parameters[Long]
    s <- ShoutoutTable if (s.recipient === ownerId && s.isViewed === false && s.isCleaned === false)
  } yield s

  def findAllForUser(user : SneekyV2User)(implicit session : Session) : List[ShoutoutResponse] = {
    unviewedByOwnerQuery(user.id.get).list map (x => {
      val s = x._1
      val u = x._2
      ShoutoutResponse(
        s.id.get,
        s.text,
        s.imageUrl,
        s.createdDate,
        s.contentType
      )
    })
  }

  def setViewed(user : SneekyV2User, id : Long)(implicit sessions : Session) : Int = {
    val q = for {
      s <- ShoutoutTable if s.recipient === user.id.get && s.id === id
    } yield s.isViewed ~ s.viewedDate

    q.update((true, Dates.nowDT))
  }

  def countSent(user : SneekyV2User)(implicit session : Session) : Int = {
    val q1 = Query(ShoutoutTable).filter(_.sender is user.id.get).length
    q1.run
  }

  def countReceived(user : SneekyV2User)(implicit session : Session) : Int = {
    val q1 = Query(ShoutoutTable).filter(_.recipient is user.id.get).length
    q1.run
  }

}
