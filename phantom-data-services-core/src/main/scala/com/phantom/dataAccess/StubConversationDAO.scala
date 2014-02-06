package com.phantom.dataAccess

import scala.slick.session.Database
import scala.concurrent._
import com.phantom.model.StubConversation

class StubConversationDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db) {

  import dal._
  import dal.profile.simple._

  private val byStubUserId = for (stubUserId <- Parameters[Long]; c <- StubConversationTable if c.toStubUser is stubUserId) yield c

  //ONLY USED BY TESTS
  def insertAll(conversations : Seq[StubConversation]) : Future[Seq[StubConversation]] = {
    future {
      db.withTransaction { implicit session => insertAllOperation(conversations) }
    }
  }

  def insertAllOperation(conversations : Seq[StubConversation])(implicit session : Session) : Seq[StubConversation] = {
    val b = StubConversationTable.forInsert.insertAll(conversations : _*)
    b.zip(conversations).map {
      case (id, conversation) =>
        conversation.copy(id = Some(id))
    }
  }

  private val byFromUser = for { fromUserId <- Parameters[Long]; c <- StubConversationTable if c.fromUser is fromUserId } yield c

  //ONLY USED BY TESTS
  def findByFromUserId(id : Long) : Future[Seq[StubConversation]] = {
    future {
      db.withSession { implicit session =>
        byFromUser(id).list
      }
    }
  }

  //the following are not in futures as they are only used by RegService which execs within a future and transaction

  def findByToStubUserIdOperation(id : Long)(implicit session : Session) : Seq[StubConversation] = byStubUserId(id).list

  def deleteOperation(ids : Seq[Long])(implicit session : Session) : Int = (for (c <- StubConversationTable if c.id inSet ids) yield c).delete
}