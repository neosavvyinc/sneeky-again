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
    val mode = AuthenticationMode(authCfg.getString("mode"))

    private def authCfg = cfg.getConfig("auth")
  }

  sealed trait AuthenticationMode

  object AuthenticationMode {
    def apply(mode : String) = mode.toLowerCase match {
      case "nohash" => NonHashingAuthentication
      case "none"   => NoAuthentication
      case "full"   => FullAuthentication
      case "debug"  => DebugAuthentication
      case _        => FullAuthentication
    }
  }

  case object FullAuthentication extends AuthenticationMode

  case object NonHashingAuthentication extends AuthenticationMode

  case object NoAuthentication extends AuthenticationMode

  case object DebugAuthentication extends AuthenticationMode

  object AWS {
    val accessKeyId = awsConfig.getString("accessKeyId")
    val secretKey = awsConfig.getString("secretKey")
    val bucket = awsConfig.getString("bucket")

    private def awsConfig = cfg.getConfig("aws")
  }

  object FileStoreConfiguration {

    val baseDirectory = fileStoreConfiguration.getString("baseDirectory")
    val baseImageUrl = fileStoreConfiguration.getString("baseImageUrl")

    private def fileStoreConfiguration = cfg.getConfig("fileStore")

  }

  object TwilioConfiguration {

    val accountSid = twilioCfg.getString("accountSid")
    val authToken = twilioCfg.getString("authToken")
    val phoneNumber = twilioCfg.getString("phoneNumber")

    private def twilioCfg = cfg.getConfig("twilio")
  }

  object ApplePushConfiguration {
    val keyStorePassword = applePushCfg.getString("keyStorePassword")
    val developmentCert = applePushCfg.getString("developmentCertPath")
    val productionCert = applePushCfg.getString("productionCertPath")
    val messageBody = applePushCfg.getString("messageBody")
    val environment = applePushCfg.getString("environment")
    val connectionCount = applePushCfg.getInt("connectionCount")

    private def applePushCfg = cfg.getConfig("apple")
  }

  object UserConfiguration {

    val invitationMax = userCfg.getInt("invitationMax")

    val maxRetries = userCfg.getInt("maxRetries")

    private def userCfg = cfg.getConfig("user")
  }

  object SecurityConfiguration {
    val sharedSecret = securityConfig.getString("sharedSecret")
    val encryptFields = securityConfig.getBoolean("encryptFields")

    private def securityConfig = cfg.getConfig("security")
  }

  object MandrillConfiguration {
    val apiKey = mandrillConfig.getString("apiKey")
    val smtpHost = mandrillConfig.getString("smtpHost")
    val smtpPort = mandrillConfig.getString("smtpPort")
    val username = mandrillConfig.getString("username")

    private def mandrillConfig = cfg.getConfig("mandrill")
  }

}
