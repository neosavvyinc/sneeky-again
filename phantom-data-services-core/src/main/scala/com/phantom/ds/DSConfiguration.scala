package com.phantom.ds

import com.typesafe.config.ConfigFactory

trait DSConfiguration {

  lazy val cfg = ConfigFactory.load().getConfig("com.phantom.ds")

  object AuthConfiguration {

    val secret = authCfg.getString("secret")
    val requestTimeout = authCfg.getLong("requestTimeout")
    val authEnabled = authCfg.getBoolean("enabled")

    private def authCfg = cfg.getConfig("auth")
  }

  object TwilioConfiguration {

    val accountSid = twilioCfg.getString("accountSid")
    val authToken = twilioCfg.getString("authToken")

    private def twilioCfg = cfg.getConfig("twilio")

  }

}