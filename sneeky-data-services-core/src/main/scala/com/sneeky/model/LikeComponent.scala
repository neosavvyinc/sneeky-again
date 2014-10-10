package com.sneeky.model

import com.sneeky.dataAccess.Profile
import org.joda.time.DateTime

import scala.slick.lifted.ColumnOption.DBType

case class Like(id : Option[Long],
                sneekId : Long,
                userId : Long)

case class Dislike(id : Option[Long],
                   sneekId : Long,
                   userId : Long)

trait LikeComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  /**
   *
   * +----------+---------+------+-----+---------+----------------+
   * | Field    | Type    | Null | Key | Default | Extra          |
   * +----------+---------+------+-----+---------+----------------+
   * | ID       | int(11) | NO   | PRI | NULL    | auto_increment |
   * | SNEEK_ID | int(11) | NO   |     | NULL    |                |
   * | USER_ID  | int(11) | NO   |     | NULL    |                |
   * +----------+---------+------+-----+---------+----------------+
   */
  object LikeTable extends Table[Like]("LIKES") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def sneekId = column[Long]("SNEEK_ID")
    def userId = column[Long]("USER_ID")

    def * = id.? ~ sneekId ~ userId <> (Like, Like.unapply _)
    def forInsert = * returning id
  }

}

trait DislikeComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  /**
   *
   * +----------+---------+------+-----+---------+----------------+
   * | Field    | Type    | Null | Key | Default | Extra          |
   * +----------+---------+------+-----+---------+----------------+
   * | ID       | int(11) | NO   | PRI | NULL    | auto_increment |
   * | SNEEK_ID | int(11) | NO   |     | NULL    |                |
   * | USER_ID  | int(11) | NO   |     | NULL    |                |
   * +----------+---------+------+-----+---------+----------------+
   */
  object DislikeTable extends Table[Dislike]("DISLIKES") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def sneekId = column[Long]("SNEEK_ID")
    def userId = column[Long]("USER_ID")

    def * = id.? ~ sneekId ~ userId <> (Dislike, Dislike.unapply _)
    def forInsert = * returning id
  }

}

