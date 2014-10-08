package com.sneeky.dataAccess

import com.sneeky.ds.framework.Dates
import com.sneeky.model.{ ShoutoutUser, BlockedUser }

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

  def findUsersWhoBlockSender(recipients : Set[ShoutoutUser], sender : ShoutoutUser)(implicit session : Session) : Set[ShoutoutUser] = {

    val recipientIds : Set[Long] = recipients.map(f => f.id.get)

    val blockedIds = for {
      b <- BlockedUserTable if (b.targetId === sender.id.get) && (b.ownerId inSet recipientIds)
      u <- UserTable if b.ownerId === u.id
    } yield u

    blockedIds.list().toSet
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
