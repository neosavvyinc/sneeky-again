package com.shoutout.dataAccess

import com.shoutout.ds.framework.Dates
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

  //Sent Count
  def sentCountForDate(date : Option[LocalDate])(implicit session : Session) : Option[Int] = {
    val dtf = DateTimeFormat.forPattern("yy-MM-dd")
    val dateSqlString = dtf.print(date.get)
    def countQuery = sql"select count(*) from SHOUTOUTS where DATE_FORMAT(CREATED_TIMESTAMP, '%yy-%MM-%dd') = $dateSqlString".as[Int]
    //println(dateSqlString)
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
  def registrationByEmailTotalCountForDate(date : Option[LocalDate])(implicit session : Session) : Option[Int] = {
    val dtf = DateTimeFormat.forPattern("yy-MM-dd")
    val dateSqlString = dtf.print(date.get)
    def countQuery = sql"select count(*) from USERS WHERE FACEBOOK_ID IS NULL AND DATE_FORMAT(CREATED_TIMESTAMP, '%yy-%MM-%dd') = $dateSqlString".as[Int]
    //println(dateSqlString)
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
  def registrationByFacebookTotalCountForDate(date : Option[LocalDate])(implicit session : Session) : Option[Int] = {
    val dtf = DateTimeFormat.forPattern("yy-MM-dd")
    val dateSqlString = dtf.print(date.get)
    def countQuery = sql"select count(*) from USERS WHERE FACEBOOK_ID IS NOT NULL AND DATE_FORMAT(CREATED_TIMESTAMP, '%yy-%MM-%dd') = $dateSqlString".as[Int]
    //println(dateSqlString)
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
