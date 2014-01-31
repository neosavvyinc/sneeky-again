package com.phantom.dataAccess

import com.phantom.model._

import scala.slick.driver.ExtendedProfile
import com.phantom.ds.framework.Logging
import java.io.{ PrintWriter, File }

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

  val writer = new PrintWriter(new File("schema.ddl"))
  ddl.createStatements.foreach(x => {
    writer.write(x + "\n")
  })
  writer.close()

  val dropWriter = new PrintWriter(new File("drop.ddl"))
  ddl.dropStatements.foreach(x => {
    dropWriter.write(x + "\n")
  })
  dropWriter.close();

  def create(db : Database) : Unit = {
    db.withTransaction { implicit session : Session =>
      try {
        ddl.create
      } catch {
        case e : Exception => new Exception("could not create table... wuh oh")
      }
    }
  }

  def drop(db : Database) : Unit = {
    db.withTransaction { implicit session : Session =>
      try {
        ddl.drop
      } catch {
        case e : Exception => {
          println(">>>> COULD NOT DROP TABLES:")
          println(e)
        }
      }
    }

  }
}
