package com.sneeky.dataAccess

import com.sneeky.ds.framework.{ Dates, Logging }
import com.sneeky.model._

import scala.concurrent.{ ExecutionContext, Future, future }
import scala.slick.session.Database

/**
 * Created by aparrish on 8/11/14.
 */
class SneekyDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  def insertSneek(shoutout : Sneek)(implicit session : Session) = {
    SneekyTable.forInsert.insert(shoutout)
  }

  private def cleanup(sneekId : Long, userId : Long)(implicit session : Session) = {
    val q1 = for {
      dislikes <- DislikeTable if dislikes.sneekId === sneekId && dislikes.userId === userId
    } yield dislikes
    q1.delete

    val q2 = for {
      likes <- LikeTable if likes.sneekId === sneekId && likes.userId === userId
    } yield likes
    q2.delete
  }

  def unlike(sneekId : Long, userId : Long)(implicit session : Session) : Int = {
    val q2 = for {
      likes <- LikeTable if likes.sneekId === sneekId && likes.userId === userId
    } yield likes
    q2.delete
  }

  def likeSneek(sneekId : Long, userId : Long)(implicit session : Session) : Unit = {
    cleanup(sneekId, userId)
    LikeTable.forInsert.insert(Like(None, sneekId, userId))
  }

  def undislike(sneekId : Long, userId : Long)(implicit session : Session) : Int = {
    val q1 = for {
      dislikes <- DislikeTable if dislikes.sneekId === sneekId && dislikes.userId === userId
    } yield dislikes
    q1.delete
  }

  def dislikeSneek(sneekId : Long, userId : Long)(implicit session : Session) : Unit = {
    cleanup(sneekId, userId)
    DislikeTable.forInsert.insert(Dislike(None, sneekId, userId))
  }

}
