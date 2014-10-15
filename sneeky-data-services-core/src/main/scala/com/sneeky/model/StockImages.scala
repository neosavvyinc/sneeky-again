package com.sneeky.model

import com.sneeky.dataAccess.Profile

case class Photo(id : Option[Long],
                 categoryId : Long,
                 isActive : Boolean,
                 url : String)

case class PhotoList(category : String, images : List[Photo])

case class PhotoCategory(id : Option[Long],
                         name : String)

case class PhotoCategoryList(photoList : List[PhotoList], name : String = "categories")

trait PhotoCategoryComponent { this : Profile =>

  import profile.simple._

  object PhotoCategoryTable extends Table[PhotoCategory]("PHOTO_CATEGORIES") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")

    def * = id.? ~ name <> (PhotoCategory, PhotoCategory.unapply _)
    def forInsert = * returning id
  }
}

/**
 * Created by aparrish on 10/14/14.
 */
trait PhotoComponent { this : Profile with PhotoCategoryComponent =>

  import profile.simple._

  object PhotoTable extends Table[Photo]("PHOTOS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def categoryId = column[Long]("CATEGORY_ID")
    def isActive = column[Boolean]("IS_ACTIVE")
    def url = column[String]("URL")

    def * = id.? ~ categoryId ~ isActive ~ url <> (Photo, Photo.unapply _)
    def forInsert = * returning id
    //    def category = foreignKey("PHOTO_CATEGORY_FK", categoryId, PhotoCategoryTable)(_.id)
  }
}
