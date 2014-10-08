package com.sneeky.ds.user

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.apache.commons.codec.binary.Base64
import com.sneeky.ds.framework.exception.ShoutoutException
import scala.concurrent.Future
import com.sneeky.ds.framework.Dates

//shamelessly stolen: http://stackoverflow.com/questions/2860943/suggestions-for-library-to-hash-passwords-in-java
object Passwords {

  val iterations = 2 //TODO: bring this back up to a respectable number
  val saltLen = 32
  val desiredKeyLen = 256

  import java.security.MessageDigest

  def generateNewPassword() : String = {
    val moment = Dates.nowDTStr
    Base64.encodeBase64String(MessageDigest.getInstance("MD5").digest(moment.getBytes))
  }

  def validate(password : String) = {
    if (password.length < 6) {
      Future.failed(ShoutoutException.passwordsComplexity)
    } else {
      Future.successful(password)
    }
  }

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