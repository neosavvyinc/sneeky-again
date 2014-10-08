package com.sneeky.ds.framework.auth

import spray.routing.authentication.Authentication
import spray.routing.AuthenticationFailedRejection
import scala.concurrent.{ ExecutionContext, Future, future }
import com.sneeky.ds.DSConfiguration
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import com.sneeky.model._
import com.sneeky.dataAccess.DatabaseSupport
import java.util.UUID
import com.sneeky.ds.framework.Logging
import org.joda.time.LocalDate
import spray.routing.RequestContext

//For now this authenticator does a bit of both authentication and authorization
//since we have no real roles or permissioning yet..just being a user opens up all doors
//hence, for every request, we opted for just one authenticator which we could use to identify a user
trait RequestAuthenticator extends Authenticator {
  def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[(ShoutoutUser, UUID)]]

  def unverified(ctx : RequestContext)(implicit ec : ExecutionContext) = request(Unverified, ctx)
  def admin(ctx : RequestContext)(implicit ec : ExecutionContext) = request(Admin, ctx)
}

trait SessionDateHashRequestAuthenticator extends RequestAuthenticator with DSConfiguration with DatabaseSupport with Logging {

  def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[(ShoutoutUser, UUID)]] = {
    log.debug(s"authenticating request ${ctx.request.uri}")
    future {
      val result = for {
        h <- extractParameter(hashP, ctx)
        d <- extractParameter(dateP, ctx)
        s <- extractParameter(sessionIdP, ctx)
        _ <- validateHash(h, d, s, ctx)
        dt <- validateTime(d, ctx)
        user <- validateSession(s, ctx)
      } yield (user, UUID.fromString(s))
      logAuthFailure(result, s"auth failed", ctx)
      val filtered = result.filter(x => ShoutoutAuthorizer.authorize(status, x._1.userStatus))
      toAuthentication(logAuthFailure(filtered, s"request was valid but the user's status was rejected", ctx))
    }
  }

  private def validateHash(clientHash : String, date : String, sessionId : String, ctx : RequestContext) : Option[String] = {
    val calculated = hashWithSecret(s"$date$delim$sessionId")
    log.debug(s"PhantomRequestAuthenticator.validateHash[calculated: $calculated and provided: $clientHash]")
    val opt = if (calculated == clientHash) { Some(date) } else { None }
    logAuthFailure(opt, "clientHash $clientHash does not match calculated hash : $calculated.", ctx)
  }

  protected def validateSession(sessionId : String, ctx : RequestContext) : Option[ShoutoutUser] = {
    val opt = sessionsDao.findFromSession(UUID.fromString(sessionId))
    logAuthFailure(opt, s"cannot find session from $sessionId", ctx)
  }

}

trait NonHashingRequestAuthenticator extends SessionDateHashRequestAuthenticator {
  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[(ShoutoutUser, UUID)]] = {

    future {
      val result = for {
        s <- extractParameter(sessionIdP, ctx)
        user <- validateSession(s, ctx)
      } yield (user, UUID.fromString(s))

      val filtered = result.filter(x => ShoutoutAuthorizer.authorize(status, x._1.userStatus))
      toAuthentication(logAuthFailure(filtered, s"request was valid but the user's status was rejected", ctx))
    }
  }
}

trait DebugAuthenticator extends NonHashingRequestAuthenticator {

  private object FullAuth extends SessionDateHashRequestAuthenticator

  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[(ShoutoutUser, UUID)]] = {
    log.debug(s"DEBUG auth for request ${ctx.request.uri}")
    val actualResults = super.request(status, ctx)
    actualResults
  }
}

trait SuppliedUserRequestAuthenticator extends RequestAuthenticator {
  // :( this hurts..cannot run in parallel w/ this ever
  var authedUser : Option[(ShoutoutUser, UUID)] = Option.empty

  override def request(status : UserStatus, ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[(ShoutoutUser, UUID)]] = {
    Future.successful(authedUser.toRight(AuthenticationFailedRejection(CredentialsRejected, Nil)))
  }
}
