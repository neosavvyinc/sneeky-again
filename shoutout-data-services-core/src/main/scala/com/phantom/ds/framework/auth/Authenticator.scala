package com.phantom.ds.framework.auth

import com.phantom.ds.DSConfiguration
import org.joda.time.DateTime
import scala.util.Try
import java.security.MessageDigest
import spray.routing.{ RequestContext, AuthenticationFailedRejection }
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import com.phantom.ds.framework.{ Dates, Logging }

trait Authenticator extends DSConfiguration with Logging {

  val hashP = "hash"
  val dateP = "date"
  val sessionIdP = "sessionId"
  val delim = "_"
  val rejected = AuthenticationFailedRejection(CredentialsRejected, Nil)

  protected def toAuthentication[T](opt : Option[T]) = opt.toRight(rejected)

  protected def validateTime(date : String, ctx : RequestContext) : Option[DateTime] = {
    val parsed = Try(Dates.readDateTime(date)).toOption
    log.debug("date parsed from client: " + parsed)

    val now = Dates.nowDT

    log.debug("now is: " + now)
    log.debug("configured requestTimeout is: " + AuthConfiguration.requestTimeout)

    val results = parsed.filter(now.getMillis - _.getMillis <= AuthConfiguration.requestTimeout)
    logAuthFailure(results, s"the interval between now $now date $parsed is greater than timeout ${AuthConfiguration.requestTimeout}", ctx)
  }

  protected def extractParameter(parameter : String, ctx : RequestContext) : Option[String] = {
    val p = ctx.request.uri.query.get(parameter)
    log.debug(s"$parameter: $p")
    logAuthFailure(p, s"expected parameter $parameter is not present", ctx)
  }

  protected def logAuthFailure[T](value : Option[T], message : String, ctx : RequestContext) : Option[T] = {
    if (value.isEmpty) {
      log.error(s"FAILING AUTHENTICATION >>> URL : ${ctx.request.uri} >>> REASON : $message")
    }
    value
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