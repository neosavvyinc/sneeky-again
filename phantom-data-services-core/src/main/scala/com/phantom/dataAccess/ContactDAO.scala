package com.phantom.dataAccess

import scala.slick.session.Database
import spray.http.{ StatusCode, StatusCodes }
import org.joda.time.LocalDate
import com.phantom.ds.framework.Logging
import com.phantom.model.Contact
import scala.concurrent.{ ExecutionContext, Future, future }

class ContactDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def insert(contact : Contact) : Contact = {
    val id = ContactTable.forInsert.insert(contact)
    contact.copy(id = Some(id))
  }

  def insertList(id : Long, ids : List[Long]) : Future[List[Contact]] = {
    future {
      //      val cs : List[Contact] = ids.map { uid : Long =>
      //        Contact(None, id, uid, "friend")
      //      }
      //
      //      cs.map(c => insert(c))
      //
      ids.map { uid : Long =>
        insert(Contact(None, id, uid, "friend"))
      }
    }

  }

  def deleteAll(id : Long)(session : scala.slick.session.Session) : Future[Int] = {
    future {
      Query(ContactTable).filter(_.ownerId === id).delete(session)
    }
  }

  def createSampleContacts = {
    ContactTable.insertAll(
      Contact(None, 1, 2, "friend"),
      Contact(None, 1, 3, "block"),
      Contact(None, 3, 2, "friend")
    )
  }
}

