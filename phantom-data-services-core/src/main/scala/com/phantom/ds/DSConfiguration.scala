package com.phantom.ds

import com.typesafe.config.ConfigFactory

trait DSConfiguration {

  lazy val cfg = ConfigFactory.load().getConfig("com.phantom.ds")

  object AuthConfiguration {

    val key = authCfg.getString("secrect")
    val requestTimeout = authCfg.getLong("requestTimeout")

    private def authCfg = cfg.getConfig("auth")
  }

}