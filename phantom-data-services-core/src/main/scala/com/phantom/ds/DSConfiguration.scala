package com.phantom.ds

import com.typesafe.config.ConfigFactory

trait DSConfiguration {

  lazy val cfg = ConfigFactory.load().getConfig("com.phantom.ds")

  object DBConfiguration {
    val statementCacheSize = dbCfg.getInt("statementCacheSize")
    val minConnectionsPerPartition = dbCfg.getInt("minConnectionsPerPartition")
    val maxConnectionsPerPartition = dbCfg.getInt("maxConnectionsPerPartition")
    val numPartitions = dbCfg.getInt("numPartitions")

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

  object TwilioConfiguration {

    val accountSid = twilioCfg.getString("accountSid")
    val authToken = twilioCfg.getString("authToken")
    def phoneNumber = twilioCfg.getString("phoneNumber")

    private def twilioCfg = cfg.getConfig("twilio")
  }

  object ApplePushConfiguration {
    val keyStorePassword = applePushCfg.getString("keyStorePassword")
    val certPath = applePushCfg.getString("certPath")

    private def applePushCfg = cfg.getConfig("apple")
  }

  object UserConfiguration {

    val invitationMax = userCfg.getInt("invitationMax")

    val maxRetries = userCfg.getInt("maxRetries")

    private def userCfg = cfg.getConfig("user")
  }

}
