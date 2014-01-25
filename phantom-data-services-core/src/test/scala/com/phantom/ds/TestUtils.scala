package com.phantom.ds

import com.phantom.model.RegistrationVerification

trait TestUtils {

  def reg(pre : String, uuid : String, post : String) = {
    val delim = "##"
    RegistrationVerification("", "", "", "", s"$pre$delim$uuid$delim$post", 1)
  }
}