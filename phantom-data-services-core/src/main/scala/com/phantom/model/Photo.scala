package com.phantom.model

import com.phantom.dataAccess.Profile

case class Photo(id : Option[Long],
                 categoryId : Long,
                 url : String)

case class Category(id : Option[Long],
                    name : String)

trait PhotoComponent { this : Profile with CategoryComponent =>

  import profile.simple._

  object PhotoTable extends Table[Photo]("PHOTOS") {
    def id = column[Long]("OWNER_ID")
    def categoryId = column[Long]("CATEGORY_ID")
    def url = column[String]("URL")

    def * = id.? ~ categoryId ~ url <> (Photo, Photo.unapply _)
    def forInsert = * returning id
    def category = foreignKey("CATEGORY_FK", categoryId, CategoryTable)(_.id)
  }
}

trait CategoryComponent { this : Profile =>

  import profile.simple._

  object CategoryTable extends Table[Category]("CATEGORIES") {
    def id = column[Long]("OWNER_ID")
    def name = column[String]("NAME")

    def * = id.? ~ name <> (Category, Category.unapply _)
    def forInsert = * returning id
  }
}
