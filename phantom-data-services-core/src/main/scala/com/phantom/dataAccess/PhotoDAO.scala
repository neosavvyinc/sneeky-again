package com.phantom.dataAccess

import scala.slick.session.Database
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.model.{ Photo, PhotoList, PhotoCategory }
import scala.concurrent.{ ExecutionContext, Future, future }

class PhotoDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

  def findAll : List[PhotoList] = {
    //    db.withSession { implicit session =>
    //      val q = (for {
    //        pc <- PhotoCategoryTable
    //        p <- PhotoTable if p.categoryId === pc.id
    //      } yield (pc.name, p)).groupBy(_._1)
    //
    //      q.list
    //    }

    List(PhotoList("someCategory", List(Photo(None, 1, true, "test"))))
  }
}
