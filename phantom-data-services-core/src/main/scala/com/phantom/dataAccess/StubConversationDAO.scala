package com.phantom.dataAccess

import scala.slick.session.Database
import scala.concurrent._
import com.phantom.model.StubConversation

class StubConversationDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db) {

  import dal._
  import dal.profile.simple._

  def insertAll(conversations : Seq[StubConversation]) : Future[Seq[StubConversation]] = {
    future {
      val b = StubConversationTable.forInsert.insertAll(conversations : _*)
      b.zip(conversations).map {
        case (id, conversation) =>
          conversation.copy(id = Some(id))
      }
    }
  }

  private val byFromUser = for { fromUserId <- Parameters[Long]; c <- StubConversationTable if c.fromUser is fromUserId } yield c

  def findByFromUserId(id : Long) : Future[Seq[StubConversation]] = {
    future {
      byFromUser(id).list
    }
  }

}