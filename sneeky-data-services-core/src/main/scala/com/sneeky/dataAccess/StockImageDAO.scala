package com.sneeky.dataAccess

import com.sneeky.ds.framework.Logging
import com.sneeky.model.{ Photo, PhotoCategory, PhotoList }

import scala.concurrent.ExecutionContext
import scala.slick.session.Database

/**
 * Created by aparrish on 10/14/14.
 */
class StockImageDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  def findAll : List[PhotoList] = {
    db.withSession { implicit session =>
      val q = (for {
        p <- PhotoTable if p.isActive
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
