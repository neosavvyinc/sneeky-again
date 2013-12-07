package com.currant.ds

import com.typesafe.config.ConfigFactory

trait DSConfiguration {

  lazy val cfg = ConfigFactory.load().getConfig("com.currant.ds")

  object DBConfig {
    private val db = cfg.getConfig("db")
    def userName : String = db.getString("user")
    def password : String = db.getString("password")
    def url : String = db.getString("url")
    def driver : String = db.getString("driver")
  }

}