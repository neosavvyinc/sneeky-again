package com.phantom.ds

import com.phantom.ds.integration.twilio.RegistrationVerification

trait TestUtils {

  def reg(pre : String, uuid : String, post : String) = {
    val delim = "##"
    RegistrationVerification("", "", "", "", s"$pre$delim$uuid$delim$post", 1)
  }
}