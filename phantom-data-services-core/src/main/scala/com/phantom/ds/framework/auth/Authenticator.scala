package com.phantom.ds.framework.auth

import com.phantom.ds.DSConfiguration
import org.joda.time.DateTime
import scala.util.Try
import java.security.MessageDigest
import spray.routing.AuthenticationFailedRejection
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import com.phantom.model.{ Stub, Unverified, Verified, UserStatus }
import com.phantom.ds.framework.{ Dates, Logging }

trait Authenticator extends DSConfiguration with Logging {

  val hashP = "hash"
  val dateP = "date"
  val sessionIdP = "sessionId"
  val delim = "_"
  val rejected = AuthenticationFailedRejection(CredentialsRejected, Nil)

  protected def toAuthentication[T](opt : Option[T]) = opt.toRight(rejected)

  protected def validateTime(date : String) : Option[DateTime] = {
    val parsed = Try(Dates.readDateTime(date)).toOption
    log.debug("date parsed from client: " + parsed)

    val now = Dates.nowDT

    log.debug("now is: " + now)
    //log.debug("now - provided: " + (now.getMillis - parsed.get.getMillis))
    log.debug("configured requestTimeout is: " + AuthConfiguration.requestTimeout)

    parsed.filter(now.getMillis - _.getMillis <= AuthConfiguration.requestTimeout)
  }

  protected def valueOf(buf : Array[Byte]) : String = buf.map("%02X" format _).mkString

  protected def hashWithSecret(clear : String, secret : String = AuthConfiguration.secret) = {
    val withSuffix = s"$clear$delim$secret"
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