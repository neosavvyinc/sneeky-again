package com.phantom.dataAccess

import com.phantom.ds.framework.exception.ShoutoutException
import com.phantom.model.{ ShoutoutUser, Group }

import scala.concurrent._
import scala.slick.session.Database

/**
 * Created by aparrish on 8/7/14.
 */
class GroupDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {

  import dal._
  import dal.profile.simple._

  private val byIdQuery = for {
    id <- Parameters[Long];
    g <- GroupTable if g.id === id
  } yield g

  def findByIdOperation(id : Long)(implicit session : Session) : Option[Group] = {
    byIdQuery(id).firstOption
  }

  def findById(id : Long) : Future[Group] = {
    future {
      db.withSession { implicit session =>
        val groupOpt = for {
          g <- findByIdOperation(id)
        } yield g

        groupOpt.getOrElse(throw ShoutoutException.nonExistentUser)
      }
    }
  }

}
