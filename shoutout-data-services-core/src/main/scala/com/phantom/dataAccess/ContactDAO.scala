package com.phantom.dataAccess

import scala.slick.session.Database
import com.phantom.ds.framework.exception.ShoutoutException
import com.phantom.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.model.Contact

class ContactDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {

  import dal._
  import dal.profile.simple._

  def insertFriendAssociation(user : ShoutoutUser, ordering : ContactOrdering, sortOrder : Int)(implicit session : Session) = {
    ContactTable.forInsert.insert(Contact(None, sortOrder, user.id.get, None, ordering.friendId, FriendType))
  }

  def insertGroupAssociation(user : ShoutoutUser, ordering : ContactOrdering, sortOrder : Int)(implicit session : Session) = {
    ContactTable.forInsert.insert(Contact(None, sortOrder, user.id.get, ordering.groupId, None, GroupType))
  }

  def deleteAllAssociationsForOwner(user : ShoutoutUser)(implicit session : Session) = {

    val q = for { c <- ContactTable if c.ownerId === user.id } yield c
    q.delete

  }

}
