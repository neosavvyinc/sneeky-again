package com.shoutout.model

import com.shoutout.dataAccess.Profile
import org.joda.time.DateTime

import scala.slick.lifted.ColumnOption.DBType

/**
 * Created by aparrish on 8/11/14.
 */
case class Shoutout(id : Option[Long],
                    sender : Long,
                    recipient : Long,
                    text : String,
                    imageUrl : String,
                    isViewed : Boolean,
                    viewedDate : Option[DateTime],
                    createdDate : DateTime,
                    isBlocked : Boolean = false)

case class ShoutoutResponse(id : Long,
                            sender : Friend,
                            text : String,
                            imageUrl : String,
                            createdDate : DateTime)

trait ShoutoutComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  /**
   * +-------------------+---------------+------+-----+---------+----------------+
   * | Field             | Type          | Null | Key | Default | Extra          |
   * +-------------------+---------------+------+-----+---------+----------------+
   * | ID                | int(11)       | NO   | PRI | NULL    | auto_increment |
   * | SENDER_ID         | int(11)       | NO   |     | NULL    |                |
   * | RECIPIENT_ID      | int(11)       | NO   |     | NULL    |                |
   * | TEXT              | varchar(1024) | NO   |     | NULL    |                |
   * | IMAGE_URL         | varchar(256)  | NO   |     | NULL    |                |
   * | IS_VIEWED         | tinyint(1)    | NO   |     | 0       |                |
   * | VIEWED_TIMESTAMP  | datetime      | YES  |     | NULL    |                |
   * | CREATED_TIMESTAMP | datetime      | NO   |     | NULL    |                |
   * | IS_BLOCKED        | tinyint(1)    | NO   |     | 0       |                |
   * +-------------------+---------------+------+-----+---------+----------------+
   */

  object ShoutoutTable extends Table[Shoutout]("SHOUTOUTS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def sender = column[Long]("SENDER_ID")
    def recipient = column[Long]("RECIPIENT_ID")
    def text = column[String]("TEXT")
    def imageUrl = column[String]("IMAGE_URL")
    def isViewed = column[Boolean]("IS_VIEWED", O.Default(false))
    def viewedDate = column[DateTime]("VIEWED_TIMESTAMP", DBType("DATETIME"))
    def createdDate = column[DateTime]("CREATED_TIMESTAMP", DBType("DATETIME"))
    def isBlocked = column[Boolean]("IS_BLOCKED")

    def * = id.? ~ sender ~ recipient ~ text ~ imageUrl ~ isViewed ~ viewedDate.? ~ createdDate ~ isBlocked <> (Shoutout, Shoutout.unapply _)
    def forInsert = * returning id

  }
}
