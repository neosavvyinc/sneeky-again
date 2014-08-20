package com.shoutout.dataAccess

import scala.slick.session.Database
import com.shoutout.ds.framework.exception.ShoutoutException
import com.shoutout.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import com.shoutout.model.Contact

class ContactDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {

  import dal._
  import dal.profile.simple._

  def findContactByUsernameForOwner(owner : ShoutoutUser, username : String)(implicit session : Session) : Option[(Contact, ShoutoutUser)] = {
    val q = for {
      username <- Parameters[String];
      c <- ContactTable if c.ownerId === owner.id.get
      u <- UserTable if u.id === c.friendId && username.toLowerCase === u.username.toLowerCase
    } yield (c, u)

    q(username).firstOption
  }

  def findContactByFacebookIdForOwner(owner : ShoutoutUser, facebookID : String)(implicit session : Session) : Option[Contact] = {
    val q = for {
      facebookId <- Parameters[String];
      c <- ContactTable if c.ownerId === owner.id.get
      u <- UserTable if u.id === c.friendId && facebookId.toLowerCase === u.facebookID.toLowerCase
    } yield c

    q(facebookID).firstOption
  }

  def insertFriendAssociation(user : ShoutoutUser, ordering : ContactOrdering, sortOrder : Int)(implicit session : Session) = {
    ContactTable.forInsert.insert(Contact(None, sortOrder, user.id.get, None, ordering.friendId, FriendType))
  }

  def insertGroupAssociation(user : ShoutoutUser, ordering : ContactOrdering, sortOrder : Int)(implicit session : Session) = {
    ContactTable.forInsert.insert(Contact(None, sortOrder, user.id.get, ordering.groupId, None, GroupType))
  }

  def countContactsForUser(user : ShoutoutUser)(implicit session : Session) : Int = {
    val q = for {
      c <- ContactTable if c.ownerId === user.id.get
    } yield c

    q.list().length
  }

  def deleteAllAssociationsForOwner(user : ShoutoutUser)(implicit session : Session) : Int = {

    val q = for { c <- ContactTable if c.ownerId === user.id } yield c
    q.delete

  }

  def deleteByFriendId(user : ShoutoutUser, friendRefId : Long)(implicit session : Session) : Int = {

    val q = for { c <- ContactTable if c.ownerId === user.id && c.friendId === friendRefId } yield c
    q.delete

  }

  def deleteByGroupId(user : ShoutoutUser, groupRefId : Long)(implicit session : Session) : Int = {

    val q = for { c <- ContactTable if c.ownerId === user.id && c.groupId === groupRefId } yield c
    q.delete

  }

  def findAllForUser(user : ShoutoutUser)(implicit session : Session) : List[Contact] = {

    val q = for {
      c <- ContactTable if c.ownerId === user.id

    } yield c
    q.list

  }

  /**
   * select U.ID, G.ID from CONTACTS C LEFT OUTER JOIN USERS U ON C.USER_REF_ID = U.ID LEFT OUTER JOIN GROUPS G ON C.GROUP_REF_ID = G.ID
   */

}
