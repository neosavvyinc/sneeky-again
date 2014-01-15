package com.phantom.ds

import com.typesafe.config.ConfigFactory

trait DSConfiguration {

  lazy val cfg = ConfigFactory.load().getConfig("com.phantom.ds")

  object DBConfiguration {

    val driver = dbCfg.getString("driver")
    val url = dbCfg.getString("url")
    val user = dbCfg.getString("user")
    val pass = dbCfg.getString("password")

    private def dbCfg = cfg.getConfig("db")
  }

  object AuthConfiguration {

    val secret = authCfg.getString("secret")
    val requestTimeout = authCfg.getLong("requestTimeout")
    val authEnabled = authCfg.getBoolean("enabled")

    private def authCfg = cfg.getConfig("auth")
  }

  object FileStoreConfiguration {

    val baseDirectory = fileStoreConfiguration.getString("baseDirectory")

    private def fileStoreConfiguration = cfg.getConfig("fileStore")

  }

}