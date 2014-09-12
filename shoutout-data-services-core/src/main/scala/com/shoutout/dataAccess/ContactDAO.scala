package com.shoutout.dataAccess

import scala.slick.jdbc.StaticQuery
import scala.slick.session.Database
import com.shoutout.ds.framework.exception.ShoutoutException
import com.shoutout.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import com.shoutout.model.Contact

class ContactDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {

  import dal._
  import dal.profile.simple._

  def findContactByIdForOwner(owner : ShoutoutUser, id : Long)(implicit session : Session) : Option[(Contact, ShoutoutUser)] = {
    val q = for {
      id <- Parameters[Long];
      c <- ContactTable if c.ownerId === owner.id.get
      u <- UserTable if u.id === c.friendId && id === u.id
    } yield (c, u)

    q(id).firstOption
  }

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

  def isFriendOfOwner(ownerUser : ShoutoutUser, targetUser : ShoutoutUser) : Boolean = {
    db.withSession { implicit session : Session =>
      val q = for { c <- ContactTable if c.ownerId === ownerUser.id && c.friendId === targetUser.id } yield c.exists
      val isFriend = q.firstOption()
      val rValue = isFriend.getOrElse(false)
      println("TargetUser: " + targetUser.id.get + " is a friend of " + ownerUser.id.get + " if " + rValue)
      rValue
    }
  }

  def isInGroupOfOwner(ownerUser : ShoutoutUser, targetUser : ShoutoutUser) : Boolean = {
    import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
    import Q.interpolation

    val ownerId = ownerUser.id.get
    val targetFriendId = targetUser.id.get

    db.withSession { implicit session : Session =>

      implicit val associationCountQueryResult = GetResult(r => MemberShip(r.<<))
      val isAssociatedByGroupQuery =
        sql"""select $targetFriendId in (select DISTINCT USER_REF_ID
              from GROUPS G join GROUP_ITEMS GI
              on G.ID = GI.GROUP_ID where OWNER_ID = $ownerId
              ORDER BY USER_REF_ID)""".as[MemberShip]

      val memberShip = isAssociatedByGroupQuery.firstOption
      println("Membership: " + memberShip)
      val rValue = memberShip.getOrElse(MemberShip(0)).count > 0
      println("TargetUser: " + targetUser.id.get + " is in a group of " + ownerUser.id.get + " if " + rValue)
      rValue
    }
  }

}
