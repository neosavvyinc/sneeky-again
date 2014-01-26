package com.phantom.dataAccess

import scala.slick.session.Database
import scala.util.{ Success, Failure }
import spray.http.{ StatusCode, StatusCodes }
import com.phantom.ds.framework.exception.PhantomException
import org.joda.time.LocalDate
import com.phantom.ds.framework.Logging
import com.phantom.model.Contact
import scala.concurrent.{ Promise, ExecutionContext, Future, future }

class ContactDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def insert(contact : Contact) : Future[Contact] = {
    future {
      ContactTable.forInsert.insert(contact) match {
        case 0         => throw PhantomException.contactNotInserted
        case id : Long => contact.copy(id = Some(id))
      }
    }
  }

  def insertAll(contacts : Seq[Contact]) : Future[Seq[Contact]] = {
    future {
      val c = ContactTable.forInsert.insertAll(contacts : _*)
      c.zip(contacts).map {
        case (id, contact) => contact.copy(id = Some(id))
      }
    }
  }

  def findByContactId(ownerId : Long, contactId : Long) : Option[Contact] = {
    val query = Query(ContactTable)
      .filter { _.ownerId === ownerId }
      .filter { _.contactId === contactId }

    println("sql: " + query.selectStatement)

    query.firstOption()
  }

  def update(contact : Contact) : Int = {
    val update = Query(ContactTable)
      .filter { _.id === contact.id }
    update.update(contact)
  }

  def deleteAll(id : Long)(session : scala.slick.session.Session) : Future[Int] = {
    future {
      Query(ContactTable).filter(_.ownerId === id).delete(session)
    }
  }

  def findAll : Future[List[Contact]] = {
    future {
      Query(ContactTable).list
    }
  }

}

