package com.phantom.dataAccess

import scala.slick.session.Database
import com.phantom.ds.framework.Logging
import java.util.UUID
import com.phantom.model.{ Verified, PhantomSession, PhantomUser, MobilePushType, Apple, Android }
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
  def findFromSession(session : UUID) : Option[PhantomUser] = {
    db.withSession { implicit s =>
      userBySessionId(session).firstOption
    }
  }

  def findTokensByUserId(userIds : Seq[Long]) : List[Option[String]] = {
    db.withSession { implicit s =>
      userIds.foreach { userId =>
        log.debug(s"finding tokens for user id $userId")
      }

      val q = for { s <- SessionTable if (s.userId inSet userIds) } yield s.pushNotifierToken.?
      val tokens = q.list

      tokens.foreach { token =>
        log.debug(s"tokens for $token")
      }

      tokens
    }
  }

  def existingSession(userId : Long) : Future[Option[PhantomSession]] = {
    future {
      db.withSession { implicit session =>
        byUserId(userId).firstOption
      }
    }
  }

  def sessionByUUID(uuid : UUID) : Future[PhantomSession] = {
    future {
      db.withSession { implicit session =>
        bySessionId(uuid).first
      }
    }
  }

  def invalidateAllForUser(id : Long) : Future[Int] = {
    future {
      db.withSession { implicit session =>
        val updateQuery = for { s <- SessionTable if s.userId === id } yield s.sessionInvalidated
        updateQuery.update(true)
      }
    }
  }

  def createSession(session : PhantomSession) : Future[PhantomSession] = {
    future {
      db.withTransaction { implicit s => createSessionOperation(session) }
    }
  }

  def createSessionOperation(session : PhantomSession)(implicit s : Session) : PhantomSession = {
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

}
