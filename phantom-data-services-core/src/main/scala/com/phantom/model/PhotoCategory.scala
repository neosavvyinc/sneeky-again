package com.phantom.model

import com.phantom.dataAccess.Profile

case class PhotoCategory(id : Option[Long],
                         name : String)

trait PhotoCategoryComponent { this : Profile =>

  import profile.simple._

  object PhotoCategoryTable extends Table[PhotoCategory]("PHOTO_CATEGORIES") {
    def id = column[Long]("OWNER_ID")
    def name = column[String]("NAME")

    def * = id.? ~ name <> (PhotoCategory, PhotoCategory.unapply _)
    def forInsert = * returning id
  }
}
