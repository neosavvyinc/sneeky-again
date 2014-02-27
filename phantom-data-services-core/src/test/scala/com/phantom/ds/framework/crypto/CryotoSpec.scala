package com.phantom.ds.framework.crypto

import org.specs2.mutable.Specification
import org.apache.commons.codec.binary.Base64
import com.phantom.ds.framework.crypto._
import com.phantom.ds.framework.protocol.defaults._
import com.phantom.ds.BasicCrypto

/**
 * Created by aparrish on 2/26/14.
 */
class CryotoSpec extends Specification with BasicCrypto {

  sequential

  "Basic Encryption and Decryption" should {

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
