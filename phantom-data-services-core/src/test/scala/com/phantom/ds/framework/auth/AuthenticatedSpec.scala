package com.phantom.ds.framework.auth

import java.security.MessageDigest
import java.net.URLEncoder
import org.apache.commons.codec.binary.Base64
import org.joda.time.{ DateTimeZone, DateTime }
import spray.routing.{ Route, AuthenticationFailedRejection }
import spray.testkit.Specs2RouteTest
import org.specs2.mutable.Specification

trait AuthenticatedSpec { this : Specification with Authenticator with Specs2RouteTest =>

  protected def hashValues(date : String, sessionId : String, secret : String = AuthConfiguration.secret) = {
    val digest = MessageDigest.getInstance("SHA-256")
    val concatBytes = s"${date}_${sessionId}_$secret".getBytes("UTF-8")
    val hashedBytes = digest.digest(concatBytes)
    URLEncoder.encode(Base64.encodeBase64String(hashedBytes), "UTF-8")
  }

  protected def hashEntryValues(date : String, secret : String = AuthConfiguration.secret) = {
    val digest = MessageDigest.getInstance("SHA-256")
    val concatBytes = s"${date}_$secret".getBytes("UTF-8")
    val hashedBytes = digest.digest(concatBytes)
    URLEncoder.encode(Base64.encodeBase64String(hashedBytes), "UTF-8")
  }

  protected def now = dateFormat.print(DateTime.now(DateTimeZone.UTC))

  protected def assertAuthFailure(url : String, route : Route) = {
    Get(url) ~> route ~> check {
      rejection must beAnInstanceOf[AuthenticationFailedRejection]
    }
  }

}