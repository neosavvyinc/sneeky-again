package com.phantom.dataAccess

import scala.slick.session.Database
import spray.http.{ StatusCode, StatusCodes }
import com.phantom.ds.framework.Logging
import com.phantom.model.{ PhantomUser, UserRegistration, ClientSafeUserResponse, UserLogin }
import scala.concurrent.{ ExecutionContext, Future }

class PhantomUserDAO(name : String, dal : DataAccessLayer, db : Database) extends BaseDAO(name, dal, db) {
  import dal._
  import dal.profile.simple._

  def createDB = dal.create
  def dropDB = dal.drop
  def purgeDB = dal.purge

  def registerUser(registrationRequest : UserRegistration) : Future[ClientSafeUserResponse] = {
    //log.info(s"registering $registrationRequest")

    var users = for {
      u <- UserTable
    } yield u

    println(users.filter(_.email === registrationRequest.email).firstOption)

    Future.successful {
      ClientSafeUserResponse(registrationRequest.email, "6148551499", registrationRequest.birthday, false, false)
    }
    // TO DO : return Future.failed if insert fails
    //    Future.successful {
    //      //      UserTable.insert(
    //      //        PhantomUser(None, registrationRequest.email, registrationRequest.birthday, true, "6148551499")
    //      //      )
    //
    //      ClientSafeUserResponse(registrationRequest.email, "6148551499", registrationRequest.birthday, false, false)
    //    }
  }

  def login(loginRequest : UserLogin) : Future[StatusCode] = {
    //log.info(s"logging in $loginRequest")
    Future.successful {
      val user = for {
        u <- UserTable if loginRequest.email == u.email
      } yield u

      //println(user.list)
      StatusCodes.OK
    }
  }

  def findContactsForUser(id : Long) : Future[StatusCode] = {
    Future.successful(StatusCodes.OK)
  }

  def createSampleUsers = {

    implicitSession.withTransaction {
      println("in a transaction...")

      UserTable.insertAll(
        PhantomUser(None, "chris@test.com", "123", true, "1234567"),
        PhantomUser(None, "adam@test.com", "123", true, "1234567")
      )

      // uncomment this, the transaction will fail and no users
      // will be inserted
      // val dumbComputation : Int = 1 / 0
    }

  }
}

