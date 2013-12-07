package com.currant.ds

import java.sql.{ BatchUpdateException, SQLException, Connection }
import org.jooq.DSLContext
import com.jolbox.bonecp.BoneCP
import org.jooq.impl.DSL
import org.jooq.exception.DataAccessException

package object db {

  trait DB {
    def withConnection[T](f : Connection => T) : T

    def withTransaction[T](f : Connection => T) : T

    def withContext[T](f : DSLContext => T) : T = {
      withConnection {
        c =>
          f(DSL.using(c))
      }
    }

    def withTransactionContext[T](f : DSLContext => T) : T = {
      withTransaction {
        c =>
          f(DSL.using(c))
      }
    }

  }

  object DB {

    final case class Config(uri : String, username : String, password : String)

    def apply(bonecp : BoneCP) = new DB {
      def withConnection[T](f : (Connection) => T) : T = {
        val c = bonecp.getConnection
        try {
          f(c)
        } finally {
          c.close()
        }
      }

      def withTransaction[T](f : (Connection) => T) : T = {
        val c = bonecp.getConnection
        try {
          val results = f(c)
          if (!c.getAutoCommit) {
            c.commit()
          }
          results
        } catch {
          case e : Exception => c.rollback(); throw e
        } finally {
          c.close()
        }
      }
    }

  }
}
