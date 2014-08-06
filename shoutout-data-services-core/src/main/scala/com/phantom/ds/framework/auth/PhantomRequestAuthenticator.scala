package com.phantom.ds.framework.auth

import spray.routing.authentication.Authentication
import spray.routing.AuthenticationFailedRejection
import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.ds.DSConfiguration
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import com.phantom.model._
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID
import com.phantom.ds.framework.Logging
import org.joda.time.LocalDate
import spray.routing.RequestContext

//For now this authenticator does a bit of both authentication and authorization
//since we have no real roles or permissioning yet..just being a user opens up all doors
//hence, for every request, we opted for just one authenticator which we could use to identify a user
trait RequestAuthenticator extends Authenticator {
  def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[ShoutoutUser]]

  def unverified(ctx : RequestContext)(implicit ec : ExecutionContext) = request(Unverified, ctx)
}

trait PhantomRequestAuthenticator extends RequestAuthenticator with DSConfiguration with DatabaseSupport with Logging {

  def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[ShoutoutUser]] = {
    log.debug(s"authenticating request ${ctx.request.uri}")
    future {
      val result = for {
        h <- extractParameter(hashP, ctx)
        d <- extractParameter(dateP, ctx)
        s <- extractParameter(sessionIdP, ctx)
        _ <- validateHash(h, d, s, ctx)
        dt <- validateTime(d, ctx)
        user <- validateSession(s, ctx)
      } yield user
      logAuthFailure(result, s"auth failed", ctx)
      toAuthentication(logAuthFailure(result, s"request was valid but the user's status was rejected", ctx))
    }
  }

  private def validateHash(clientHash : String, date : String, sessionId : String, ctx : RequestContext) : Option[String] = {
    val calculated = hashWithSecret(s"$date$delim$sessionId")
    log.debug(s"PhantomRequestAuthenticator.validateHash[calculated: $calculated and provided: $clientHash]")
    val opt = if (calculated == clientHash) { Some(date) } else { None }
    logAuthFailure(opt, "clientHash $clientHash does not match calculated hash : $calculated.", ctx)
  }

  protected def validateSession(sessionId : String, ctx : RequestContext) : Option[ShoutoutUser] = {
    val opt = sessions.findFromSession(UUID.fromString(sessionId))
    logAuthFailure(opt, s"cannot find session from $sessionId", ctx)
  }
}

trait NonHashingRequestAuthenticator extends PhantomRequestAuthenticator {
  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[ShoutoutUser]] = {

    future {
      val result = for {
        s <- extractParameter(sessionIdP, ctx)
        user <- validateSession(s, ctx)
      } yield user
      toAuthentication(result)
    }
  }
}

trait DebugAuthenticator extends NonHashingRequestAuthenticator {

  private object FullAuth extends PhantomRequestAuthenticator

  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[ShoutoutUser]] = {
    log.debug(s"DEBUG auth for request ${ctx.request.uri}")
    val actualResults = super.request(status, ctx)
    actualResults
  }
}

trait PassThroughRequestAuthenticator extends RequestAuthenticator {

  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[ShoutoutUser]] = {

    log.debug("hash: " + ctx.request.uri.query.get(hashP))
    log.debug("date: " + ctx.request.uri.query.get(dateP))
    log.debug("sessionId: " + ctx.request.uri.query.get(sessionIdP))

    val user = Some(ShoutoutUser(
      None,
      UUID.randomUUID,
      Some("fbid"),
      Some("nsauro@sauron.com"),
      Some("password"),
      Some(new LocalDate(2003, 12, 21)),
      Some("firstName"),
      Some("lastName"),
      "username",
      Some("blah"),
      true)
    )

    Future.successful(user.toRight(AuthenticationFailedRejection(CredentialsRejected, Nil)))
  }

}

trait SuppliedUserRequestAuthenticator extends RequestAuthenticator {
  // :( this hurts..cannot run in parallel w/ this ever
  var authedUser : Option[ShoutoutUser] = Option.empty

  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[ShoutoutUser]] = {
    Future.successful(authedUser.toRight(AuthenticationFailedRejection(CredentialsRejected, Nil)))
  }
}
