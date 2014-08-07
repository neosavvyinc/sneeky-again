package com.phantom.dataAccess

import com.phantom.ds.framework.exception.ShoutoutException
import com.phantom.model.{ GroupItem, ShoutoutUser, Group }

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

  def insertGroupOperation(group : Group)(implicit session : Session) : Group = {
    val id = GroupTable.forInsert.insert(group)
    group.copy(id = Some(id))
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

  def findMembers(groupId : Long)(implicit session : Session) : List[ShoutoutUser] = {
    membersByGroupIdQuery(groupId).list
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
