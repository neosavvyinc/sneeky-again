package com.shoutout.ds.framework.auth

import scala.concurrent._
import spray.routing.authentication._
import com.shoutout.ds.DSConfiguration
import spray.routing.RequestContext

trait EntryPointAuthenticator extends Authenticator {
  def enter(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[Boolean]]
}

trait PhantomEntryPointAuthenticator extends EntryPointAuthenticator with DSConfiguration {

  def enter(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[Boolean]] = {
    log.debug(s"authenticating request ${ctx.request.uri}")
    future {
      val result = for {
        h <- extractParameter(hashP, ctx)
        d <- extractParameter(dateP, ctx)
        _ <- validateHash(h, d, ctx)
        dt <- validateTime(d, ctx)
      } yield true
      toAuthentication(logAuthFailure(result, s"auth failed", ctx))
    }
  }

  private def validateHash(clientHash : String, date : String, ctx : RequestContext) = {
    val calculated = hashWithSecret(date)
    log.debug(s"PhantomEntryPointAuthenticator.validateHash[calculated: $calculated and provided: $clientHash]")
    val results = if (hashWithSecret(date) == clientHash) {
      Some(date)
    } else {
      None
    }
    logAuthFailure(results, s"supplied hash $clientHash did not match the expected hash when hashing the date with secret $date", ctx)
  }
}

trait PassThroughEntryPointAuthenticator extends EntryPointAuthenticator {
  def enter(ctx : RequestContext)(implicit ec : ExecutionContext) : Future[Authentication[Boolean]] = {
    Future.successful(Right(true))
  }
}