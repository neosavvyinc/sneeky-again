package com.phantom.ds.framework.auth

import spray.routing.HttpService
import spray.http.StatusCodes
import scala.concurrent.ExecutionContext.Implicits.global

trait AuthTestPoint extends HttpService {
  this : RequestAuthenticator with EntryPointAuthenticator =>

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

  val testRoute = entryRoute

}