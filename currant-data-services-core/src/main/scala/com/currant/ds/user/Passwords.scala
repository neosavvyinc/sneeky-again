package com.currant.ds.user

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.apache.commons.codec.binary.Base64

//shamelessly stolen: http://stackoverflow.com/questions/2860943/suggestions-for-library-to-hash-passwords-in-java
object Passwords {

  val iterations = 10 * 1024
  val saltLen = 32
  val desiredKeyLen = 256

  def getSaltedHash(password : String) = {
    val salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLen)
    // store the salt with the password
    Base64.encodeBase64String(salt) + "$" + hash(password, salt)
  }

  def check(password : String, stored : String) : Boolean = {
    val saltAndPass = stored.split("\\$")
    if (saltAndPass.length != 2) {
      false
    } else {
      val hashOfInput = hash(password, Base64.decodeBase64(saltAndPass(0)))
      hashOfInput.equals(saltAndPass(1))
    }

  }

  // using PBKDF2 from Sun, an alternative is https://github.com/wg/scrypt
  // cf. http://www.unlimitednovelty.com/2012/03/dont-use-bcrypt.html
  private def hash(password : String, salt : Array[Byte]) : String = {
    val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val key = f.generateSecret(new PBEKeySpec(
      password.toCharArray, salt, iterations, desiredKeyLen)
    )
    Base64.encodeBase64String(key.getEncoded)
  }

}