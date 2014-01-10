package com.phantom.dataAccess

import scala.slick.session.Database
import spray.http.{ StatusCode, StatusCodes }
import org.joda.time.LocalDate
import com.phantom.ds.framework.Logging
//import com.phantom.model.{ PhantomUser, UserRegistration, ClientSafeUserResponse, UserLogin }
import scala.concurrent.{ ExecutionContext, Future }

class ContactDAO(name : String, dal : DataAccessLayer, db : Database) extends BaseDAO(name, dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  //  def registerUser(registrationRequest : UserRegistration) : Future[ClientSafeUserResponse] = {
  //    //log.info(s"registering $registrationRequest")
  //
  //    Query(UserTable).filter(_.email === registrationRequest.email)
  //      .firstOption.map { u : PhantomUser =>
  //        Future.failed(new Exception())
  //      }.getOrElse {
  //        //
  //        // confirm / enter confirmation code service ?
  //        //
  //        UserTable.insert(PhantomUser(None, registrationRequest.email, new LocalDate(12345678), true, "6148551499"))
  //        Future.successful(ClientSafeUserResponse(registrationRequest.email, "6148551499", registrationRequest.birthday, false, false))
  //      }
  //  }

}

