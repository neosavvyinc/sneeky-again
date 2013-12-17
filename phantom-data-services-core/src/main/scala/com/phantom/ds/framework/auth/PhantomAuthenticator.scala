package com.phantom.ds.framework.auth

import spray.routing.authentication.Authentication
import spray.routing.{ AuthenticationFailedRejection, RequestContext }
import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.model.User
import com.phantom.ds.DSConfiguration
import java.security.MessageDigest
import org.joda.time.format.ISODateTimeFormat
import scala.util.Try
import org.joda.time.{ DateTimeZone, DateTime }
import spray.routing.AuthenticationFailedRejection.CredentialsRejected

//For now this authenticator does a bit of both authentication and authorziation
//since we have no real roles or permissioning yet..just being a user opens up all doors
//hence, for every request, we opted for just one authenticator which we could use to identify a user
trait Authenticator {
  def phantom(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[User]]
}

trait PhantomAuthenticator extends Authenticator with DSConfiguration {
  sessionRepo : SessionRepository =>

  val hashedP = "hashed"
  val dateP = "date"
  val sessionIdP = "sessionId"
  val delim = "_"
  val digest = MessageDigest.getInstance("SHA-256")
  val dateFormat = ISODateTimeFormat.basicDateTime

  def rejected = AuthenticationFailedRejection(CredentialsRejected, Nil)

  def phantom(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[User]] = {

    future {
      val result = for {
        h <- ctx.request.uri.query.get(hashedP)
        d <- ctx.request.uri.query.get(dateP)
        s <- ctx.request.uri.query.get(sessionIdP)
        _ <- validateHash(h, d, s)
        dt <- validateTime(d)
        user <- validateSession(s)
      } yield user
      result.toRight(rejected)
    }
  }

  private def validateHash(clientHash : String, date : String, sessionId : String) = {

    val clientBytes = clientHash.getBytes("UTF-8")
    val concatBytes = s"$date$delim$sessionId".getBytes("UTF-8")
    val hashedBytes = digest.digest(concatBytes)
    if (hashedBytes == clientBytes) Some(date) else None
  }

  private def validateTime(date : String) : Option[DateTime] = {
    val parsed = Try(dateFormat.parseDateTime(date)).toOption
    val now = DateTime.now(DateTimeZone.UTC).getMillis
    parsed.filter(now - _.getMillis <= AuthConfiguration.requestTimeout)
  }

  private def validateSession(sessionId : String) : Option[User] = {
    getUser(sessionId)
  }

}