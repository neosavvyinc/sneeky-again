package com.phantom.ds.framework.crypto

import org.specs2.mutable.Specification
import org.apache.commons.codec.binary.Base64
import com.phantom.ds.framework.crypto._
import com.phantom.ds.framework.protocol.defaults._
import com.phantom.ds.BasicCrypto
import java.security.MessageDigest
import com.phantom.ds.framework.Dates

/**
 * Created by aparrish on 2/26/14.
 */
class CryptoSpec extends Specification with BasicCrypto {

  val delim = "_"
  val secret = "x48qHCRrDN"

  def hashWithSecret(clear : String) = {
    val withSuffix = s"$clear$delim$secret"
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = withSuffix.getBytes("UTF-8")
    val digested = digest.digest(bytes)
    valueOf(digested)
  }

  def valueOf(buf : Array[Byte]) : String = buf.map("%02X" format _).mkString

  sequential

  "Basic Encryption and Decryption" should {

    "test a basic local date crypto" in {
      val date = Dates.nowLD
      val encrypted = encryptLocalDate(date)
      val decrypted = decryptField(encrypted)

      Dates.write(date) shouldEqual decrypted

    }

    "test the hashWithSecret function" in {
      val hashed = hashWithSecret("2014-02-27T20:48:00-05:00")

      hashed shouldEqual "71e3ece80e9bf8fa521488fa0a578dcfc19c89199c0f24783b8fe6884cb9cb0a"
    }.pendingUntilFixed("This is a helper test")

    "properly take in an input and create a predictable output" in {

      val encrypted = encryptField("adam")
      println(encrypted)

      val decrypted = decryptField(encrypted)
      println(decrypted)

      "adam" shouldEqual decrypted
    }

  }

  "Test some strings from Dave" should {

    "properly take a phone number and make it an encrypted value" in {

      val encrypted = encryptField("+19197419597")
      println("Encrypted Phone number: " + encrypted)

      val decrypted = decryptField(encrypted)
      println("Decrypted Phone number: " + decrypted)

      val encryptedText = "PeBpC3zbtW7sfjeVznXH5g=="

      println("decrypted phone number: " + decryptField(encryptedText))

      "+19197419597" shouldEqual decrypted
    }

  }

}
