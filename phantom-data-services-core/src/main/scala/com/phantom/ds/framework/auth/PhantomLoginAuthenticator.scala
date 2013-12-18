package com.phantom.ds.framework.auth

import scala.concurrent._
import spray.routing.authentication._
import com.phantom.ds.DSConfiguration
import spray.routing.RequestContext

trait EntryPointAuthenticator extends Authenticator {
  def enter(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[Boolean]]
}

trait PhantomEntryPointAuthenticator extends EntryPointAuthenticator with DSConfiguration {

  def enter(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[Boolean]] = {
    future {
      val result = for {
        h <- ctx.request.uri.query.get(hashP)
        d <- ctx.request.uri.query.get(dateP)
        _ <- validateHash(h, d)
        dt <- validateTime(d)
      } yield true
      toAuthentication(result)
    }
  }

  private def validateHash(clientHash : String, date : String) = {
    if (hashWithSecret(date) == clientHash) {
      Some(date)
    } else {
      None
    }
  }
}

trait PassThroughEntryPointAuthenticator extends EntryPointAuthenticator {
  def enter(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[Boolean]] = {
    Future.successful(Right(true))
  }
}