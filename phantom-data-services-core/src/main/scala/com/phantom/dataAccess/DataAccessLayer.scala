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
    with UserSessionComponent {

  import profile.simple._

  def ddl =
    UserTable.ddl ++
      ConversationTable.ddl ++
      ConversationItemTable.ddl ++
      ContactTable.ddl ++
      SessionTable.ddl

  //  val writer = new PrintWriter(new File("schema.ddl"))
  //  writer.write("drop database phantom;\n")
  //  writer.write("create database phantom;\n")
  //  writer.write("use phantom;\n")
  //  ddl.createStatements.foreach(x => {
  //    val clean = x.replaceAll("`", "")
  //    writer.write(clean + ";\n")
  //  })
  //  writer.close()

  //  val dropWriter = new PrintWriter(new File("drop.ddl"))
  //  ddl.dropStatements.foreach(x => {
  //    dropWriter.write(x + "\n")
  //  })
  //  dropWriter.close();

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
          log.debug(e.toString())
        }
      }
    }

  }
}
