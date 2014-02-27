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
import com.phantom.model.PhantomUser
import spray.routing.RequestContext

//For now this authenticator does a bit of both authentication and authorization
//since we have no real roles or permissioning yet..just being a user opens up all doors
//hence, for every request, we opted for just one authenticator which we could use to identify a user
trait RequestAuthenticator extends Authenticator {
  def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[PhantomUser]]

  def unverified(ctx : RequestContext)(implicit ec : ExecutionContext) = request(Unverified, ctx)

  def verified(ctx : RequestContext)(implicit ec : ExecutionContext) = request(Verified, ctx)

  def stub(ctx : RequestContext)(implicit ec : ExecutionContext) = request(Stub, ctx)

}

trait PhantomRequestAuthenticator extends RequestAuthenticator with DSConfiguration with DatabaseSupport with Logging {

  def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[PhantomUser]] = {

    log.debug("hash: " + ctx.request.uri.query.get(hashP))
    log.debug("date: " + ctx.request.uri.query.get(dateP))
    log.debug("sessionId: " + ctx.request.uri.query.get(sessionIdP))

    future {
      val result = for {
        h <- ctx.request.uri.query.get(hashP)
        d <- ctx.request.uri.query.get(dateP)
        s <- ctx.request.uri.query.get(sessionIdP)
        _ <- validateHash(h, d, s)
        dt <- validateTime(d)
        user <- validateSession(s)
      } yield user
      val filtered = result.filter(x => PhantomAuthorizer.authorize(status, x.status))
      toAuthentication(filtered)
    }
  }

  private def validateHash(clientHash : String, date : String, sessionId : String) = {
    val calculated = hashWithSecret(s"$date$delim$sessionId")
    log.debug(s"PhantomRequestAuthenticator.validateHash[calculated: $calculated and provided: $clientHash]")
    if (calculated == clientHash) {
      Some(date)
    } else {
      None
    }
  }

  protected def validateSession(sessionId : String) : Option[PhantomUser] = {
    sessions.findFromSession(UUID.fromString(sessionId))
  }
}

trait NonHashingRequestAuthenticator extends PhantomRequestAuthenticator {
  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[PhantomUser]] = {

    log.debug("hash: " + ctx.request.uri.query.get(hashP))
    log.debug("date: " + ctx.request.uri.query.get(dateP))
    log.debug("sessionId: " + ctx.request.uri.query.get(sessionIdP))

    future {
      val result = for {
        s <- ctx.request.uri.query.get(sessionIdP)
        user <- validateSession(s)
      } yield user
      val filtered = result.filter(x => PhantomAuthorizer.authorize(status, x.status))
      toAuthentication(filtered)
    }
  }
}

trait PassThroughRequestAuthenticator extends RequestAuthenticator {

  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[PhantomUser]] = {

    log.debug("hash: " + ctx.request.uri.query.get(hashP))
    log.debug("date: " + ctx.request.uri.query.get(dateP))
    log.debug("sessionId: " + ctx.request.uri.query.get(sessionIdP))

    val user = Some(PhantomUser(None, UUID.randomUUID, Some("nsauro@sauron.com"), Some("password"), Some(new LocalDate(2003, 12, 21)), true, Some(""), Verified))
    Future.successful(user.toRight(AuthenticationFailedRejection(CredentialsRejected, Nil)))
  }

}

trait SuppliedUserRequestAuthenticator extends RequestAuthenticator {
  // :( this hurts..cannot run in parallel w/ this ever
  var authedUser : Option[PhantomUser] = Option.empty

  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[PhantomUser]] = {
    Future.successful(authedUser.toRight(AuthenticationFailedRejection(CredentialsRejected, Nil)))
  }
}
