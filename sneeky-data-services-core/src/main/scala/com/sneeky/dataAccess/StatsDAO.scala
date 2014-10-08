package com.sneeky.dataAccess

import com.sneeky.ds.framework.Dates
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

import scala.concurrent.ExecutionContext
import scala.slick.session.{ Session, Database }
import scala.slick.jdbc.{ StaticQuery => Q }
import com.github.tototoshi.slick.JodaSupport._
import Q.interpolation

/**
 * Created by Dave on 8/25/14.
 */
class StatsDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db) {

  val defaultTimeZone = "EST"
  val defaultPattern = "yy-MM-dd"
  val sourceTimezone = "'UTC'"

  //Sent Count
  def sentCountForDate(date : Option[LocalDate], targetTimezone : Option[String])(implicit session : Session) : Option[Int] = {
    val dtf = DateTimeFormat.forPattern(defaultPattern)
    val dateSqlString = "'" + dtf.print(date.get) + "'"
    val targetTimezoneString = "'" + targetTimezone.getOrElse(defaultTimeZone) + "'"
    def countQuery = sql"select count(*) from SHOUTOUTS where DATE_FORMAT(CONVERT_TZ(CREATED_TIMESTAMP, #$sourceTimezone, #$targetTimezoneString), '%y-%m-%d') = #$dateSqlString".as[Int]

    try {
      val result = countQuery.firstOption
      result
    } catch {
      case e : Exception => {
        println("Error occurred:  " + e.toString)
        Some(0)
      }
    }
  }

  def sentAllCount(implicit session : Session) : Option[Int] = {
    def countQuery = sql"select count(*) from SHOUTOUTS".as[Int]
    try {
      val result = countQuery.firstOption
      result
    } catch {
      case e : Exception => {
        println("Error occurred:  " + e.toString)
        Some(0)
      }
    }
  }

  //Email Registration
  def registrationByEmailTotalCountForDate(date : Option[LocalDate], targetTimezone : Option[String])(implicit session : Session) : Option[Int] = {
    import scala.slick.jdbc.{ GetResult, StaticQuery => Q }

    val dtf = DateTimeFormat.forPattern(defaultPattern)
    val dateSqlString = "'" + dtf.print(date.get) + "'"
    val targetTimezoneString = "'" + targetTimezone.getOrElse(defaultTimeZone) + "'"
    def countQuery = sql"select count(*) from USERS WHERE FACEBOOK_ID IS NULL AND DATE_FORMAT(CONVERT_TZ(CREATED_TIMESTAMP, #$sourceTimezone, #$targetTimezoneString), '%y-%m-%d') = #$dateSqlString".as[Int]

    try {
      val result = countQuery.firstOption
      result
    } catch {
      case e : Exception => {
        println("Error occurred:  " + e.toString)
        Some(0)
      }
    }
  }

  def registrationByEmailTotalCount(implicit session : Session) : Option[Int] = {
    def countQuery = sql"select count(*) from USERS WHERE FACEBOOK_ID IS NULL".as[Int]
    try {
      val result = countQuery.firstOption
      result
    } catch {
      case e : Exception => {
        println("Error occurred:  " + e.toString)
        Some(0)
      }
    }
  }

  //Facebook Registration
  def registrationByFacebookTotalCountForDate(date : Option[LocalDate], targetTimezone : Option[String])(implicit session : Session) : Option[Int] = {
    val dtf = DateTimeFormat.forPattern(defaultPattern)
    val dateSqlString = "'" + dtf.print(date.get) + "'"
    val targetTimezoneString = "'" + targetTimezone.getOrElse(defaultTimeZone) + "'"
    def countQuery = sql"select count(*) from USERS WHERE FACEBOOK_ID IS NOT NULL AND DATE_FORMAT(CONVERT_TZ(CREATED_TIMESTAMP, #$sourceTimezone, #$targetTimezoneString), '%y-%m-%d') = #$dateSqlString".as[Int]

    try {
      val result = countQuery.firstOption
      result
    } catch {
      case e : Exception => {
        println("Error occurred:  " + e.toString)
        Some(0)
      }
    }
  }

  def registrationByFacebookTotalCount(implicit session : Session) : Option[Int] = {
    def countQuery = sql"select count(*) from USERS WHERE FACEBOOK_ID IS NOT NULL".as[Int]
    try {
      val result = countQuery.firstOption
      result
    } catch {
      case e : Exception => {
        println("Error occurred:  " + e.toString)
        Some(0)
      }
    }
  }
}
