package com.sneeky.model

import com.sneeky.dataAccess.Profile
import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import scala.slick.lifted.ColumnOption.DBType

case class GroupResponse(id : Long,
                         ownerId : Long,
                         name : String,
                         members : List[Friend])

case class Group(id : Option[Long],
                 ownerId : Long,
                 name : String)

case class GroupItem(id : Option[Long],
                     groupId : Long,
                     userRefId : Long)

trait GroupComponent { this : Profile =>

  import profile.simple._

  /**
   * +------------+--------------+------+-----+---------+----------------+
   * | Field      | Type         | Null | Key | Default | Extra          |
   * +------------+--------------+------+-----+---------+----------------+
   * | ID         | int(11)      | NO   | PRI | NULL    | auto_increment |
   * | OWNER_ID   | int(11)      | NO   |     | NULL    |                |
   * | GROUP_NAME | varchar(256) | NO   |     | NULL    |                |
   * +------------+--------------+------+-----+---------+----------------+
   */

  object GroupTable extends Table[Group]("GROUPS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def ownerId = column[Long]("OWNER_ID")
    def groupName = column[String]("GROUP_NAME", DBType("VARCHAR(256)"))

    def * = id.? ~ ownerId ~ groupName <> (Group, Group.unapply _)
    def forInsert = * returning id
  }

  /**
   * +-------------+---------+------+-----+---------+----------------+
   * | Field       | Type    | Null | Key | Default | Extra          |
   * +-------------+---------+------+-----+---------+----------------+
   * | ID          | int(11) | NO   | PRI | NULL    | auto_increment |
   * | GROUP_ID    | int(11) | NO   |     | NULL    |                |
   * | USER_REF_ID | int(11) | NO   |     | NULL    |                |
   * +-------------+---------+------+-----+---------+----------------+
   */
  object GroupItemTable extends Table[GroupItem]("GROUP_ITEMS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def groupId = column[Long]("GROUP_ID")
    def userRefId = column[Long]("USER_REF_ID", DBType("VARCHAR(256"))

    def * = id.? ~ groupId ~ userRefId <> (GroupItem, GroupItem.unapply _)
    def forInsert = * returning id

  }
}