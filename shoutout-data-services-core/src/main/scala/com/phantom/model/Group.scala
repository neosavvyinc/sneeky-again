package com.phantom.model

import com.phantom.dataAccess.Profile
import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import scala.slick.lifted.ColumnOption.DBType

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
}