package com.phantom.dataAccess

import scala.slick.session.Database
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.model.Contact

class ContactDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

  //ONLY USED BY TESTS
  def insert(contact : Contact) : Future[Contact] = {
    future {
      db.withTransaction { implicit session => insertOperation(contact) }
    }
  }

  def insertOperation(contact : Contact)(implicit session : Session) : Contact = {
    ContactTable.forInsert.insert(contact) match {
      case 0         => throw PhantomException.contactNotInserted
      case id : Long => contact.copy(id = Some(id))
    }
  }

  def insertAllOperation(contacts : Seq[Contact])(implicit session : Session) : Seq[Contact] = {
    val c = ContactTable.forInsert.insertAll(contacts : _*)
    c.zip(contacts).map {
      case (id, contact) => contact.copy(id = Some(id))
    }
  }

  //TODO ONLY USED BY TESTS
  def insertAll(contacts : Seq[Contact]) : Seq[Contact] = {
    db.withTransaction { implicit session => insertAllOperation(contacts) }
  }

  def findByContactId(ownerId : Long, contactId : Long) : Future[Contact] = {
    future {
      db.withSession { implicit session =>
        val q = for {
          c <- ContactTable if c.ownerId === ownerId && c.contactId === contactId
        } yield c

        q.firstOption
          .map((c : Contact) => c)
          .getOrElse(throw PhantomException.nonExistentContact)
      }
    }
  }

  def filterConnectedToContactOperation(ownerIds : Set[Long], contactId : Long)(implicit session : Session) = {
    val q = for {
      (u, c) <- UserTable join ContactTable on (_.id === _.ownerId) if (u.id inSet ownerIds) && (c.contactId is contactId)
    } yield u.id
    q.list
  }

  def update(contact : Contact) : Future[Int] = {
    future {
      db.withTransaction { implicit session =>
        val q = for {
          c <- ContactTable if c.id === contact.id
        } yield c.ownerId ~ c.contactId ~ c.contactType
        q.update(contact.ownerId, contact.contactId, contact.contactType)
      }
    }
  }

  def clearBlockListOperation(id : Long)(implicit session : Session) : Int = {
    val q = for { c <- ContactTable if c.ownerId === id && c.contactType === (Blocked : ContactType) } yield c
    q.delete
  }

  def blockContactByUserIdOperation(ownerId : Long, contactId : Long)(implicit session : Session) : Int = {
    val q = for {
      c <- ContactTable if c.ownerId === ownerId && c.contactId === contactId
    } yield c.contactType
    q.update(Blocked)
  }

  def blockContactsOperation(ids : Set[Long])(implicit session : Session) : Int = {
    val q = for {
      c <- ContactTable if c.id inSet ids
    } yield c.contactType
    q.update(Blocked)
  }

  private val byOwnerQuery = for {
    owner <- Parameters[Long]
    c <- ContactTable
    u <- UserTable if (u.id === c.contactId) && (c.ownerId === owner)
  } yield (c, u)

  def findAllForOwnerOperation(ownerId : Long)(implicit session : Session) : List[(Contact, PhantomUser)] = {
    byOwnerQuery(ownerId).list()
  }

  def findAllForOwnerInSetOperation(ownerId : Long, numbers : Set[String])(implicit session : Session) : List[(Contact, PhantomUser)] = {
    val q = for {
      c <- ContactTable
      u <- UserTable if (u.id is c.contactId) && (c.ownerId is ownerId) && (u.phoneNumber inSet numbers)
    } yield (c, u)

    q.list()
  }

  def findAllWhoBlockUserOperation(userId : Long, numbers : Set[String])(implicit session : Session) : List[(Contact, PhantomUser)] = {
    val q = for {
      c <- ContactTable
      u <- UserTable if (u.id is c.ownerId) && (u.phoneNumber inSet numbers) && (c.contactId is userId) && (c.contactType is (Blocked : ContactType))
    } yield (c, u)

    q.list()

  }

  def deleteAllUnblockedOperation(ownerId : Long)(implicit session : Session) : Int = {

    val q = for { c <- ContactTable if c.ownerId === ownerId && (c.contactType === (Friend : ContactType)) } yield c
    q.delete
  }

  //ONLY USED BY TESTS
  def findAll : List[Contact] = {
    db.withSession { implicit session =>
      Query(ContactTable).list
    }
  }

  //ONLY USED BY TESTS
  def findAllForOwner(id : Long) : Seq[(Contact, PhantomUser)] = {
    db.withSession { implicit session => findAllForOwnerOperation(id) }
  }
}
