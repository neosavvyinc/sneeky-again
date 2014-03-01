package com.phantom.ds

import org.apache.commons.codec.binary.Base64
import com.phantom.ds.framework.crypto._
import com.phantom.ds.framework.protocol.defaults._

/**
 * Created by aparrish on 2/26/14.
 */
trait BasicCrypto extends DSConfiguration {

  def encodeBase64(bytes : Array[Byte]) = Base64.encodeBase64String(bytes)
  def decodeBase64(bytes : Array[Byte]) = Base64.decodeBase64(bytes)

  def encryptField(fieldValue : String) : String = {
    if (SecurityConfiguration.encryptFields)
      encodeBase64(AES.encrypt(fieldValue, SecurityConfiguration.sharedSecret))
    else
      fieldValue
  }

  def encryptOption(field : Option[String]) : Option[String] = {
    if (SecurityConfiguration.encryptFields) {
      field match {
        case Some(x) => Option(encryptField(x))
        case None    => Option("")
      }
    } else
      field
  }

  def encryptDate() = ???

  def decryptField(fieldValue : String) : String = {
    if (SecurityConfiguration.encryptFields)
      new String(AES.decrypt(decodeBase64(fieldValue.getBytes()), SecurityConfiguration.sharedSecret))
    else
      fieldValue
  }

}
