package com.phantom.ds.framework.auth

import com.phantom.ds.DSConfiguration
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ DateTimeZone, DateTime }
import scala.util.Try
import java.security.MessageDigest
import org.apache.commons.codec.binary.Base64
import spray.routing.AuthenticationFailedRejection
import spray.routing.AuthenticationFailedRejection.CredentialsRejected

trait Authenticator extends DSConfiguration {

  val hashP = "hash"
  val dateP = "date"
  val sessionIdP = "sessionId"
  val delim = "_"
  val dateFormat = ISODateTimeFormat.basicDateTime
  val rejected = AuthenticationFailedRejection(CredentialsRejected, Nil)

  protected def toAuthentication[T](opt : Option[T]) = opt.toRight(rejected)

  protected def validateTime(date : String) : Option[DateTime] = {
    val parsed = Try(dateFormat.parseDateTime(date)).toOption
    val now = DateTime.now(DateTimeZone.UTC).getMillis
    parsed.filter(now - _.getMillis <= AuthConfiguration.requestTimeout)
  }

  protected def hashWithSecret(clear : String) = {
    val withSuffix = s"$clear$delim${AuthConfiguration.secret}"
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = withSuffix.getBytes("UTF-8")
    Base64.encodeBase64String(digest.digest(bytes))
  }

}