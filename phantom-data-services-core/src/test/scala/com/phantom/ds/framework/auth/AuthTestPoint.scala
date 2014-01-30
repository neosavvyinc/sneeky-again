package com.phantom.ds.framework.auth

import spray.routing.HttpService
import spray.http.StatusCodes
import scala.concurrent.ExecutionContext.Implicits.global

trait AuthTestPoint extends HttpService with PhantomRequestAuthenticator with PhantomEntryPointAuthenticator {

  val testRoute =
    pathPrefix("test" / "protected") {
      get {
        authenticate(request _) {
          user =>
            complete {
              StatusCodes.OK
            }
        }
      }
    } ~
      pathPrefix("test" / "entry") {
        post {
          authenticate(enter _) {
            b =>
              complete {
                StatusCodes.OK
              }
          }
        }
      }

}