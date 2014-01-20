package com.phantom.dataAccess

import scala.slick.session.Database
import scala.util.{ Success, Failure }
import spray.http.{ StatusCode, StatusCodes }
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

    val id = ContactTable.forInsert.insert(contact)
    id match {
      // fix this for failure case... I believe insert returns a 0
      // on failure???
      //case 0 => insertPromise.failure(new Exception("unable to insert contact"))
      case _ => Future.successful(contact.copy(id = Some(id)))
    }
  }

  def insertList(id : Long, ids : List[Long]) : Future[List[Contact]] = {

    val contactList : Promise[List[Contact]] = Promise()

    future {

      val insertFutures : List[Future[Contact]] = ids.map { uid : Long =>
        insert(Contact(None, id, uid, "friend"))
      }

      Future.sequence(insertFutures).onComplete {
        case Success(vs) => contactList.success(vs)
        case Failure(ex) => contactList.failure(ex)
      }
      // is it possible to batch insert?
      //      cs.foreach { (c : Contact) =>
      //        insert(c).onComplete {
      //          case Success(v)  => contactList.success(List(v))
      //          case Failure(ex) => contactList.failure(ex)
      //        }
      //      }
    }

    contactList.future
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

