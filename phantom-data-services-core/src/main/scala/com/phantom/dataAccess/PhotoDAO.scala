package com.phantom.dataAccess

import scala.slick.session.Database
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.model.{ Photo, PhotoList, PhotoCategory }
import scala.concurrent.{ ExecutionContext, Future, future }

case class TestThing(categoryId : Long, images : List[Photo])

class PhotoDAO(dal : DataAccessLayer, db : Database)(implicit ex : ExecutionContext) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

  def findAll : List[PhotoList] = {
    db.withSession { implicit session =>
      val q = (for {
        p <- PhotoTable
        pc <- PhotoCategoryTable if p.categoryId === pc.id
      } yield (pc.name, p))

      val photoLists = q.list.groupBy(_._1).map {
        case (k, v) => PhotoList(k, v.map(_._2))
      }
      photoLists.toList
    }
  }

  def insertCategory(pc : PhotoCategory) : PhotoCategory = {
    db.withTransaction { implicit session =>
      PhotoCategoryTable.forInsert.insert(pc) match {
        case 0         => throw new Exception("could not insert photo category")
        case id : Long => pc.copy(id = Some(id))
      }
    }
  }

  def insertPhoto(p : Photo) : Photo = {
    db.withTransaction { implicit session =>
      PhotoTable.forInsert.insert(p) match {
        case 0         => throw new Exception("could not insert photo")
        case id : Long => p.copy(id = Some(id))
      }
    }
  }

  def findById(id : Long) : Option[Photo] = {
    db.withSession { implicit session =>
      Query(PhotoTable).filter(_.id === id).firstOption
    }
  }
}
