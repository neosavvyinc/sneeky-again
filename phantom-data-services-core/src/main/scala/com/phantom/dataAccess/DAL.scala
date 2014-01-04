package com.phantom.dataAccess

import scala.slick.driver.ExtendedProfile
import scala.slick.session.{ Session, Database }

import com.phantom.model.UserComponent

import scala._

trait Profile {
  val profile : ExtendedProfile
}

class DAL(override val profile : ExtendedProfile) extends Profile with UserComponent {
  import profile.simple._

  def ddl = UserTable.ddl

  ddl.createStatements.foreach(println)

  def create(implicit session : Session) : Unit = {
    try {
      ddl.create
    } catch {
      case e : Exception => new Exception("could not create table... wuh oh")
    }
  }

  def drop(implicit session : Session) : Unit = {
    try {
      ddl.drop
    } catch {
      case e : Exception => new Exception("could not drop table... dang")
    }
  }

  def purge(implicit session : Session) : Unit = { drop; create }
}