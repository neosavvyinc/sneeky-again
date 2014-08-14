package com.shoutout.dataAccess

import com.shoutout.ds.framework.Dates
import com.shoutout.model.BlockedUser

import scala.concurrent.ExecutionContext
import scala.slick.session.Database

/**
 * Created by aparrish on 8/14/14.
 */
class BlockUserDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {

  import dal._
  import dal.profile.simple._

  def insertBlock(ownerId : Long, targetId : Long) : Option[BlockedUser] = {
    db.withTransaction { implicit session : Session =>
      val preInsertValue = BlockedUser(None, ownerId, targetId, Dates.nowDT)
      val id = BlockedUserTable.forInsert.insert(preInsertValue)
      Some(preInsertValue.copy(id = Some(id)))
    }
  }

  def findBlockByOwnerAndTargetId(ownerId : Long, targetId : Long) : Option[BlockedUser] = {
    db.withSession { implicit session : Session =>

      val block = for {
        b <- BlockedUserTable if b.ownerId === ownerId && b.targetId === targetId
      } yield b

      block.firstOption

    }
  }

  def clearBlocksForOwner(ownerId : Long) : Int = {
    db.withSession { implicit session : Session =>

      val block = for {
        b <- BlockedUserTable if b.ownerId === ownerId
      } yield b

      block.delete
    }
  }
}
