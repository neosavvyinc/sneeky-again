package com.sneeky.dataAccess

import com.sneeky.ds.framework.exception.ShoutoutException
import com.sneeky.model.{ GroupItem, ShoutoutUser, Group }

import scala.concurrent._
import scala.slick.session.Database

/**
 * Created by aparrish on 8/7/14.
 */
class GroupDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {

  import dal._
  import dal.profile.simple._

  private val membersByGroupIdQuery = for {
    groupId <- Parameters[Long]
    gi <- GroupItemTable if gi.groupId is groupId
    u <- UserTable if gi.userRefId === u.id
  } yield u

  private val groupItemsByGroupIdQuery = for {
    groupId <- Parameters[Long]
    gi <- GroupItemTable if gi.groupId === groupId
  } yield gi

  private val byIdQuery = for {
    id <- Parameters[Long]
    g <- GroupTable if g.id === id
  } yield g

  def findByIdOperation(id : Long)(implicit session : Session) : Option[Group] = {
    byIdQuery(id).firstOption
  }

  def updateGroupOperation(groupUpdate : Group)(implicit session : Session) : Int = {
    val persistentGroup = findByIdOperation(groupUpdate.id.get)
    persistentGroup match {
      case None => 0
      case Some(pGroup) => {
        val q = for { group <- GroupTable if group.id === groupUpdate.id } yield group
        q.update(pGroup.copy(name = groupUpdate.name))
      }
    }
  }

  def insertGroupOperation(group : Group)(implicit session : Session) : Group = {
    val id = GroupTable.forInsert.insert(group)
    group.copy(id = Some(id))
  }

  def groupExistsOperation(id : Long)(implicit session : Session) : Option[Boolean] = {
    val groupExistsQuery = for {
      g <- GroupTable if g.id === id
    } yield g.exists
    groupExistsQuery.firstOption
  }

  def isGroupValid(id : Long)(implicit session : Session) : Option[Group] = {
    val groupByGroupIdQuery = for {
      gi <- GroupTable if gi.id === id
    } yield gi

    groupByGroupIdQuery.firstOption
  }

  def insertGroupItemOperation(item : GroupItem)(implicit session : Session) : GroupItem = {
    val id = GroupItemTable.forInsert.insert(item)
    item.copy(id = Some(id))
  }

  def deleteMembersOperation(groupId : Long)(implicit session : Session) : Int = {
    val groupItemsByGroupIdQuery = for {
      gi <- GroupItemTable if gi.groupId === groupId
    } yield gi
    groupItemsByGroupIdQuery.delete
  }

  def deleteGroup(groupId : Long)(implicit session : Session) : Int = {
    val groupByGroupIdQuery = for {
      gi <- GroupTable if gi.id === groupId
    } yield gi
    groupByGroupIdQuery.delete
  }

  def findMembers(groupId : Long)(implicit session : Session) : List[ShoutoutUser] = {
    membersByGroupIdQuery(groupId).list
  }

  def findMembersForGroups(groupIds : Set[Long])(implicit session : Session) : List[ShoutoutUser] = {
    val q = for {
      gi <- GroupItemTable if gi.groupId inSet groupIds
      u <- UserTable if gi.userRefId === u.id
    } yield u

    q.list
  }

  def isOwnerOfGroup(groupId : Long, ownerId : Long) : Boolean = {
    db.withSession { implicit session =>

      val ownerCheck = for {
        g <- GroupTable if g.ownerId === ownerId && g.id === groupId
      } yield g

      val group = ownerCheck.firstOption
      group match {
        case Some(g) => true
        case None    => false
      }
    }
  }

  def findById(id : Long) : Future[Group] = {
    future {
      db.withSession { implicit session =>
        val groupOpt = for {
          g <- findByIdOperation(id)
        } yield g

        groupOpt.getOrElse(throw ShoutoutException.groupNotFoundException)
      }
    }
  }

}
