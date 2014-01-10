package com.phantom.dataAccess

import com.phantom.model.{
  UserComponent,
  ConversationComponent,
  ConversationItemComponent,
  ContactComponent
}

import scala.slick.driver.ExtendedProfile
import scala.slick.session.{ Session, Database }

import scala._
import org.joda.time.LocalDate

trait Profile {
  val profile : ExtendedProfile
}

class DataAccessLayer(override val profile : ExtendedProfile) extends Profile
    with UserComponent
    with ConversationComponent
    with ConversationItemComponent
    with ContactComponent {
  import profile.simple._

  def ddl = ConversationItemTable.ddl ++
    ConversationTable.ddl ++
    UserTable.ddl ++
    ContactTable.ddl

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
