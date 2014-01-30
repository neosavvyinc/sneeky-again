package com.phantom.dataAccess

import com.phantom.model._

import scala.slick.driver.ExtendedProfile
import com.phantom.ds.framework.Logging

trait Profile {
  val profile : ExtendedProfile
}

class DataAccessLayer(override val profile : ExtendedProfile) extends Profile with Logging
    with UserComponent
    with ConversationComponent
    with ConversationItemComponent
    with ContactComponent
    with UserSessionComponent
    with StubUserComponent
    with StubConversationComponent {

  import profile.simple._

  def ddl =
    UserTable.ddl ++
      ConversationTable.ddl ++
      ConversationItemTable.ddl ++
      ContactTable.ddl ++
      SessionTable.ddl ++
      StubUserTable.ddl ++
      StubConversationTable.ddl

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
        println(">>>> COULD NOT DROP TABLES:")
        println(e)
      }
    }
  }

  def purge(implicit session : Session) : Unit = { drop; create }
}
