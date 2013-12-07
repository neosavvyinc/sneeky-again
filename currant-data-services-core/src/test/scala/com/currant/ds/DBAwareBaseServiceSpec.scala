package com.currant.ds

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeExample

import spray.testkit.Specs2RouteTest
import com.currant.ds.db.DB
import com.jolbox.bonecp.{ BoneCP, BoneCPConfig }
import java.sql.ResultSet
import com.currant.ds.framework.Logging

/**
 * Created by Neosavvy
 * Kick off tests 1
 *
 * User: adamparrish
 * Date: 11/20/13
 * Time: 8:17 AM
 */
trait DBAwareBaseServiceSpec extends Specification with DSConfiguration with Specs2RouteTest with BeforeExample with Logging {

  def actorRefFactory = system

  def dbScripts : Set[String] = Set.empty

  def db : DB = {
    val bcpCfg = new BoneCPConfig()
    bcpCfg.setUser(DBConfig.userName)
    bcpCfg.setPassword(DBConfig.password)
    bcpCfg.setJdbcUrl(DBConfig.url)
    bcpCfg.setDefaultAutoCommit(false)
    bcpCfg.setDisableJMX(true)

    val bcp = new BoneCP(bcpCfg)

    DB(bcp)
  }

  def before = {
    val bcp = setupNewConnectionPool()
    log.debug("resetting the db...")
    executeBatch("/drop-ddl.sql", bcp)
    executeBatch("/ddl.sql", bcp)
    executeScripts(bcp)
    bcp.close()
    bcp.shutdown()
  }

  def setupNewConnectionPool() : BoneCP = {
    val bcpCfg = new BoneCPConfig()
    bcpCfg.setUser(DBConfig.userName)
    bcpCfg.setPassword(DBConfig.password)
    bcpCfg.setJdbcUrl(DBConfig.url)
    bcpCfg.setDefaultAutoCommit(false)
    bcpCfg.setDisableJMX(true)
    new BoneCP(bcpCfg)
  }

  def executeBatch(ddl : String, bcp : BoneCP) {
    val conn = bcp.getConnection
    try {
      log.debug(s"executing $ddl")

      conn.setAutoCommit(false)
      val queries = QueryReader.fromFile(ddl)
      queries.foreach {
        q =>
          log.debug(s"executing query $q")
          val st = conn.createStatement()
          st.execute(q)
      }

      conn.commit()
      conn.setAutoCommit(true)
    } catch {
      case e : Exception => log.error("Caught an error: " + e.toString)
    } finally {
      conn.close()
    }

  }

  def executeScripts(bcp : BoneCP) {
    dbScripts.foreach(executeBatch(_, bcp))
  }

  def executeCountForTable(table : String, bcp : BoneCP = setupNewConnectionPool) : Integer = {
    val sql = "SELECT COUNT(*) AS COUNT FROM " + table
    val conn = bcp.getConnection
    val rs : ResultSet = conn.createStatement.executeQuery(sql)
    rs.next()
    rs.getInt("count")
  }

}
