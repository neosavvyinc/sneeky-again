package com.phantom.dataAccess

import scala.slick.session.Database
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.model.Contact
import scala.concurrent.{ ExecutionContext, Future, future }

class ContactDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

  //ONLY USED BY TESTS
  def insert(contact : Contact) : Future[Contact] = {
    future {
      db.withTransaction { implicit session =>
        ContactTable.forInsert.insert(contact) match {
          case 0         => throw PhantomException.contactNotInserted
          case id : Long => contact.copy(id = Some(id))
        }
      }
    }
  }

  //TODO OPERATION ME
  def insertAll(contacts : Seq[Contact]) : Future[Seq[Contact]] = {
    future {
      db.withTransaction { implicit session =>
        val c = ContactTable.forInsert.insertAll(contacts : _*)
        c.zip(contacts).map {
          case (id, contact) => contact.copy(id = Some(id))
        }
      }
    }
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

  //TODO OPERATION ME
  def deleteAll(id : Long)(session : scala.slick.session.Session) : Future[Int] = {
    future {
      db.withTransaction { implicit session =>
        Query(ContactTable).filter(_.ownerId === id).delete(session)
      }
    }
  }

  //ONLY USED BY TESTS
  def findAll : Future[List[Contact]] = {
    future {
      db.withSession { implicit session =>
        Query(ContactTable).list
      }
    }
  }

}

