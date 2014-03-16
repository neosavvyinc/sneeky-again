package com.phantom.ds.photo

import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.model._
import com.phantom.ds.framework.Logging
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.framework.exception.PhantomException

trait PhotoService {
  def findAll : Future[List[PhotoList]]
}

object PhotoService {

  def apply()(implicit ec : ExecutionContext) = new PhotoService with DatabaseSupport with Logging {

    def findAll : Future[List[PhotoList]] = {
      future {
        photoDao.findAll
      }
    }
  }
}
