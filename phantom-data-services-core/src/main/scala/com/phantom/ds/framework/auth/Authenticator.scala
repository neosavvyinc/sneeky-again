package com.phantom.ds.framework.auth

import com.phantom.ds.DSConfiguration
import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }
import org.joda.time.{ DateTimeZone, DateTime }
import scala.util.Try
import java.security.MessageDigest
import org.apache.commons.codec.binary.Base64
import spray.routing.AuthenticationFailedRejection
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import com.phantom.model.{ Stub, Unverified, Verified, UserStatus }
import com.phantom.ds.framework.Logging

trait Authenticator extends DSConfiguration with Logging {

  val hashP = "hash"
  val dateP = "date"
  val sessionIdP = "sessionId"
  val delim = "_"
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  val rejected = AuthenticationFailedRejection(CredentialsRejected, Nil)

  protected def toAuthentication[T](opt : Option[T]) = opt.toRight(rejected)

  protected def validateTime(date : String) : Option[DateTime] = {
    val parsed = Try(dateFormat.parseDateTime(date)).toOption
    log.debug("date parsed from client: " + parsed)

    val now = DateTime.now()

    log.debug("now is: " + now)
    log.debug("now - provided: " + (now.getMillis - parsed.get.getMillis))
    log.debug("configured requestTimeout is: " + AuthConfiguration.requestTimeout)

    parsed.filter(now.getMillis - _.getMillis <= AuthConfiguration.requestTimeout)
  }

  protected def valueOf(buf : Array[Byte]) : String = buf.map("%02X" format _).mkString

  protected def hashWithSecret(clear : String) = {
    val withSuffix = s"$clear$delim${AuthConfiguration.secret}"
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = withSuffix.getBytes("UTF-8")
    val digested = digest.digest(bytes)
    valueOf(digested)
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