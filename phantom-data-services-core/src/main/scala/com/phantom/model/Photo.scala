package com.phantom.model

import com.phantom.dataAccess.Profile

case class Photo(id : Option[Long],
                 categoryId : Long,
                 url : String)

trait PhotoComponent { this : Profile with PhotoCategoryComponent =>

  import profile.simple._

  object PhotoTable extends Table[Photo]("PHOTOS") {
    def id = column[Long]("OWNER_ID")
    def categoryId = column[Long]("CATEGORY_ID")
    def url = column[String]("URL")

    def * = id.? ~ categoryId ~ url <> (Photo, Photo.unapply _)
    def forInsert = * returning id
    def category = foreignKey("PHOTO_CATEGORY_FK", categoryId, PhotoCategoryTable)(_.id)
  }
}
