package com.sneeky.dataAccess

import scala.slick.session.Database
import com.sneeky.ds.framework.{ Dates, Logging }
import java.util.UUID
import com.sneeky.model.{ SneekySession, SneekyV2User, MobilePushType }
import scala.concurrent.{ ExecutionContext, Future, future }

class SessionDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  private val userBySessionId = for {
    id <- Parameters[UUID]
    (s, u) <- SessionTable innerJoin UserTable on ((sess, user) => sess.userId === user.id && sess.sessionId === id)
  } yield u

  private val byUserId = for {
    id <- Parameters[Long]
    (s, u) <- SessionTable innerJoin UserTable on ((sess, user) => sess.userId === user.id && sess.userId === id)
  } yield s

  private val bySessionId = for {
    uuid <- Parameters[UUID]
    s <- SessionTable if s.sessionId === uuid
  } yield s

  //TODO future me
  def findFromSession(session : UUID) : Option[SneekyV2User] = {
    db.withSession { implicit s =>
      userBySessionId(session).firstOption
    }
  }

  def findFromPushNotifierAndType(pushNotifier : String, pushType : MobilePushType) : List[SneekySession] = {
    db.withSession { implicit s =>
      val q = for { s <- SessionTable if (s.pushNotifierToken === pushNotifier && s.pushNotifierType === pushType) } yield s
      q.list()
    }
  }

  def findTokensForId(userId : Long) : List[Option[String]] = {
    db.withSession { implicit session : Session =>
      val q = for { s <- SessionTable if s.userId === userId } yield s.pushNotifierToken.?

      log.trace("statement we are executing is " + q.selectStatement)

      val tokens = q.list

      log.trace(s"found tokens: $tokens")

      tokens

    }
  }

  def findTokensByUserId(userIds : Seq[Long]) : Map[Long, Set[String]] = {

    db.withSession { implicit session : Session =>

      userIds.foreach { userId =>
        log.debug(s"finding tokens for user id $userId")
      }

      val q = for { s <- SessionTable if (s.userId inSet userIds) } yield s.userId ~ s.pushNotifierToken.?

      log.trace("statement we are executing is " + q.selectStatement)

      val tokens = q.list

      log.trace(s"found tokens: $tokens")

      val grouped = tokens.groupBy(_._1).mapValues(x => x.map(_._2).flatten.toSet)
      grouped.foreach { case (k, v) => log.debug(s"tokens for $k are $v") }
      grouped

    }

  }

  def existingSession(userId : Long) : Future[Option[SneekySession]] = {
    future {
      db.withSession { implicit session =>
        byUserId(userId).firstOption
      }
    }
  }

  def sessionByUUID(uuid : UUID) : Future[SneekySession] = {
    future {
      db.withSession { implicit session =>
        bySessionId(uuid).first
      }
    }
  }

  def createSession(session : SneekySession) : Future[SneekySession] = {
    future {
      db.withTransaction { implicit s => createSessionOperation(session) }
    }
  }

  def createSessionOperation(session : SneekySession)(implicit s : Session) : SneekySession = {
    SessionTable.insert(session)
    session
  }

  def removeSession(sessionId : UUID) : Future[Int] = {
    log.trace(s"deleting $sessionId")
    future {
      db.withTransaction { implicit session =>
        Query(SessionTable).where(_.sessionId === sessionId).delete
      }
    }
  }

  def updatePushNotifier(sessionId : UUID, pushNotifier : String, pushNotifierType : MobilePushType) : Boolean = {

    db.withSession { implicit session =>
      val upQuery = for { s <- SessionTable if s.sessionId is sessionId } yield s.pushNotifierToken ~ s.pushNotifierType
      val numRows = upQuery.update(pushNotifier, pushNotifierType)
      numRows > 0
    }

  }

  def updateLastAccessed(sessionId : UUID) : Boolean = {

    db.withSession { implicit session =>
      val upQuery = for { s <- SessionTable if s.sessionId is sessionId } yield s.lastAccessed
      val numRows = upQuery.update(Dates.nowDT)
      numRows > 0
    }

  }

}
