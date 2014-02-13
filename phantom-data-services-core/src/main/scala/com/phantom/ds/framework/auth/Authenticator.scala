package com.phantom.ds.framework.auth

import com.phantom.ds.DSConfiguration
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ DateTimeZone, DateTime }
import scala.util.Try
import java.security.MessageDigest
import org.apache.commons.codec.binary.Base64
import spray.routing.AuthenticationFailedRejection
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import com.phantom.model.{ Stub, Unverified, Verified, UserStatus }

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

/*very simple authorizer for now...will keep us afloat until we have more stringent requirements
this just assumes the following auth hierarchy:
- Verified -> Top dawg, can hit all points
- Unverified -> can only hit endpoints for Unverified and Stubs
- Stub       -> can only hit endpoints for Stus
*/
object PhantomAuthorizer {

  private val weights : Map[UserStatus, Int] = Map(Verified -> 100, Unverified -> 10, Stub -> 1)

  def authorize(securityRole : UserStatus, userRole : UserStatus) : Boolean = {
    val securityWeight = weights.getOrElse(securityRole, -1)
    val userWeight = weights.getOrElse(userRole, -1)
    userWeight >= securityWeight
  }

}