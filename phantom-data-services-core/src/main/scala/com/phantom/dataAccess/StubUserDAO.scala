package com.phantom.dataAccess

import scala.slick.session.Database
import scala.concurrent._
import com.phantom.ds.framework.Logging
import com.phantom.model.StubUser

class StubUserDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  private val byPhoneNumberQuery = for (phoneNumber <- Parameters[String]; u <- StubUserTable if u.phoneNumber is phoneNumber) yield u

  def insertAll(stubUsers : Seq[StubUser]) : Future[Seq[StubUser]] = {
    future {
      db.withTransaction { implicit session =>
        val b = StubUserTable.forInsert.insertAll(stubUsers : _*)
        b.zip(stubUsers).map {
          case (id, stubUser) =>
            stubUser.copy(id = Some(id))
        }
      }
    }
  }

  def findByPhoneNumbers(phoneNumbers : Set[String]) : Future[List[StubUser]] = {
    future {
      db.withSession { implicit session =>
        val q = for { u <- StubUserTable if u.phoneNumber inSet phoneNumbers } yield u
        q.list
      }
    }
  }

  //how in the hell do i exec update blank from users set invite = invite +1 where id in(..) ?
  def updateInvitationCount(users : Seq[StubUser]) : Future[Int] = {
    future {
      db.withTransaction { implicit session =>
        val ids = users.map(_.id.get).mkString(",")
        val statement = session.conn.createStatement()
        val q = s"update STUB_USERS set invitation_count = invitation_count + 1 where id in ($ids)"
        statement.executeUpdate(q)
      }
    }
  }

  //the following are not within futures as they are only used by RegistrationService within a future/transaction

  def findByPhoneNumberOperation(phoneNumber : String)(implicit session : Session) : Option[StubUser] = byPhoneNumberQuery(phoneNumber).firstOption

  def deleteOperation(id : Long)(implicit session : Session) : Int = (for (u <- StubUserTable if u.id is id) yield u).delete

}