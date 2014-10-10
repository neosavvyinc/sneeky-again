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

  def countUnread(user : SneekyV2User) : Int = {
    import scala.slick.jdbc.{ StaticQuery => Q }
    import Q.interpolation

    db.withSession { implicit session : Session =>

      /**
       * For some reason we are seeing that this query is not reading the recently inserted
       * row from the outer calling function. This should allow it to see even non-committed
       * data from the outer transaction
       */
      session.conn.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED)

      val id = user.id.get
      val userName = user.username

      def countQuery = sql"select count(*) from SHOUTOUTS where RECIPIENT_ID = $id and IS_VIEWED = false and IS_BLOCKED = false and IS_CLEANED = false".as[Int]
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
