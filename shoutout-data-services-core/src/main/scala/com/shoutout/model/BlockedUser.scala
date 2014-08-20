package com.shoutout.model

import com.shoutout.dataAccess.Profile
import org.joda.time.DateTime

case class BlockedUser(id : Option[Long],
                       ownerId : Long,
                       targetId : Long,
                       created : DateTime)

trait BlockedUserComponent { this : Profile with UserComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  /**
   * +-------------------+----------+------+-----+---------+----------------+
   * | Field             | Type     | Null | Key | Default | Extra          |
   * +-------------------+----------+------+-----+---------+----------------+
   * | ID                | int(11)  | NO   | PRI | NULL    | auto_increment |
   * | OWNER_ID          | int(11)  | NO   |     | NULL    |                |
   * | TARGET_ID         | int(11)  | NO   |     | NULL    |                |
   * | CREATED_TIMESTAMP | datetime | NO   |     | NULL    |                |
   * +-------------------+----------+------+-----+---------+----------------+
   */
  object BlockedUserTable extends Table[BlockedUser]("BLOCKED_USERS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def ownerId = column[Long]("OWNER_ID")
    def targetId = column[Long]("TARGET_ID")
    def created = column[DateTime]("CREATED_TIMESTAMP")

    def * = id.? ~ ownerId ~ targetId ~ created <> (BlockedUser.apply _, BlockedUser.unapply _)
    def forInsert = * returning id
  }

}
