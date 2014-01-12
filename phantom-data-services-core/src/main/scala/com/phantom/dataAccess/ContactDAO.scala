package com.phantom.dataAccess

import scala.slick.session.Database
import spray.http.{ StatusCode, StatusCodes }
import org.joda.time.LocalDate
import com.phantom.ds.framework.Logging
import com.phantom.model.Contact
import scala.concurrent.{ ExecutionContext, Future }

class ContactDAO(dal : DataAccessLayer, db : Database) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def insert(contact : Contact) = {
    val id = ContactTable.forInsert.insert(contact)
    Contact(Some(id), contact.ownerId, contact.contactId, contact.contactType)
  }

  def deleteAll(id : Long)(session : scala.slick.session.Session) : StatusCode = {
    Query(ContactTable).filter(_.ownerId === id).delete(session) match {
      case 0 =>
        new Exception("nothing deleted"); StatusCodes.NotFound
      case _ => StatusCodes.OK
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

