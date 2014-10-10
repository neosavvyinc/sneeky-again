package com.sneeky.model

import com.sneeky.dataAccess.Profile
import org.joda.time.DateTime

import scala.slick.lifted.ColumnOption.DBType

/**
 * Created by aparrish on 8/11/14.
 */
case class Sneek(id : Option[Long],
                 sender : Long,
                 text : String,
                 imageUrl : String,
                 createdDate : DateTime)

case class SneekResponse(id : Long,
                         text : String,
                         imageUrl : String,
                         createdDate : DateTime,
                         contentType : String)

trait SneekComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  /**
   *  +-------------------+---------------+------+-----+---------+----------------+
   *  | Field             | Type          | Null | Key | Default | Extra          |
   *  +-------------------+---------------+------+-----+---------+----------------+
   *  | ID                | int(11)       | NO   | PRI | NULL    | auto_increment |
   *  | SENDER_ID         | int(11)       | NO   |     | NULL    |                |
   *  | TEXT              | varchar(1024) | NO   |     | NULL    |                |
   *  | IMAGE_URL         | varchar(256)  | NO   |     | NULL    |                |
   *  | CREATED_TIMESTAMP | datetime      | NO   |     | NULL    |                |
   *  +-------------------+---------------+------+-----+---------+----------------+
   */

  object SneekyTable extends Table[Sneek]("SNEEKS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def sender = column[Long]("SENDER_ID")
    def text = column[String]("TEXT")
    def imageUrl = column[String]("IMAGE_URL")
    def createdDate = column[DateTime]("CREATED_TIMESTAMP", DBType("DATETIME"))

    def * = id.? ~ sender ~ text ~ imageUrl ~ createdDate <> (Sneek, Sneek.unapply _)
    def forInsert = * returning id

  }
}
