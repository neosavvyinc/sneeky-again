package com.phantom.ds.framework.auth

import spray.routing.HttpService
import spray.http.StatusCodes
import scala.concurrent.ExecutionContext.Implicits.global

trait AuthTestPoint extends HttpService {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  def protectedRoute = pathPrefix("test" / "protected") {
    get {
      authenticate(verified _) {
        user =>
          complete {
            StatusCodes.OK
          }
      }
    }
  }

  def entryRoute = pathPrefix("test" / "entry") {
    post {
      authenticate(enter _) {
        b =>
          complete {
            StatusCodes.OK
          }
      }
    }
  }

  def unverifiedRoute = pathPrefix("test" / "unverified") {
    get {
      authenticate(unverified _) {
        user =>
          complete {
            StatusCodes.OK
          }
      }
    }
  }

  val testRoute = protectedRoute ~ entryRoute ~ unverifiedRoute

}