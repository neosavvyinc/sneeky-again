package com.phantom.dataAccess

import com.phantom.model._

import scala.slick.driver.ExtendedProfile
import scala.slick.session.{ Session, Database }

import scala._
import org.joda.time.LocalDate
import com.phantom.ds.framework.Logging

trait Profile {
  val profile : ExtendedProfile
}

class DataAccessLayer(override val profile : ExtendedProfile) extends Profile with Logging
    with UserComponent
    with ConversationComponent
    with ConversationItemComponent
    with ContactComponent
    with UserSessionComponent {

  import profile.simple._

  def ddl =
    UserTable.ddl ++
      ConversationTable.ddl ++
      ConversationItemTable.ddl ++
      ContactTable.ddl ++
      SessionTable.ddl

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
      case e : Exception => {
        new Exception("could not drop table... dang")
        println(">>>> COULD NOT DROP TABLES:")
        println(e)
      }
    }
  }

  def purge(implicit session : Session) : Unit = { drop; create }
}
