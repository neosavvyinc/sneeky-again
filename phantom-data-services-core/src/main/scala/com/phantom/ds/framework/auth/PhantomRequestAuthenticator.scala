package com.phantom.ds.framework.auth

import spray.routing.authentication.Authentication
import spray.routing.{ AuthenticationFailedRejection, RequestContext }
import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.ds.DSConfiguration
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import com.phantom.model.PhantomUser

//For now this authenticator does a bit of both authentication and authorziation
//since we have no real roles or permissioning yet..just being a user opens up all doors
//hence, for every request, we opted for just one authenticator which we could use to identify a user
trait RequestAuthenticator extends Authenticator {
  def request(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[PhantomUser]]
}

trait PhantomRequestAuthenticator extends RequestAuthenticator with DSConfiguration {
  sessionRepo : SessionRepository =>

  def request(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[PhantomUser]] = {

    future {
      val result = for {
        h <- ctx.request.uri.query.get(hashP)
        d <- ctx.request.uri.query.get(dateP)
        s <- ctx.request.uri.query.get(sessionIdP)
        _ <- validateHash(h, d, s)
        dt <- validateTime(d)
        user <- validateSession(s)
      } yield user
      toAuthentication(result)
    }
  }

  private def validateHash(clientHash : String, date : String, sessionId : String) = {
    if (hashWithSecret(s"$date$delim$sessionId") == clientHash) {
      Some(date)
    } else {
      None
    }
  }

  private def validateSession(sessionId : String) : Option[PhantomUser] = {
    getUser(sessionId)
  }
}

trait PassThroughRequestAuthenticator extends PhantomRequestAuthenticator with MockSessionRepository {

  override def request(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[PhantomUser]] = {
    Future.successful(getUser("").toRight(AuthenticationFailedRejection(CredentialsRejected, Nil)))
  }

}