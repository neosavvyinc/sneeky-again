package com.phantom.dataAccess

import scala.slick.session.Database
import scala.concurrent.{ Future, ExecutionContext, future }

class BaseDAO(dal : DataAccessLayer, db : Database) {

  def transact[T](f : => T)(implicit ec : ExecutionContext) : Future[T] = {
    future {
      db.withTransaction {
        implicit session =>
          f
      }
    }
  }

}
