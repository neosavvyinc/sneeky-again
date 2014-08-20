package com.shoutout.ds.framework.auth

import spray.routing.{ Route, AuthenticationFailedRejection }
import spray.testkit.Specs2RouteTest
import org.specs2.mutable.Specification
import com.shoutout.ds.framework.Dates

trait AuthenticatedSpec { this : Specification with Authenticator with Specs2RouteTest =>

  protected def hashValues(date : String, sessionId : String, secret : String = AuthConfiguration.secret) = {
    hashWithSecret(s"$date$delim$sessionId", secret)
  }

  protected def hashEntryValues(date : String, secret : String = AuthConfiguration.secret) = {
    hashWithSecret(date, secret)
  }

  protected def now = Dates.nowDTStr

  protected def assertAuthFailure(url : String, route : Route) = {
    Get(url) ~> route ~> check {
      rejection must beAnInstanceOf[AuthenticationFailedRejection]
    }
  }

}