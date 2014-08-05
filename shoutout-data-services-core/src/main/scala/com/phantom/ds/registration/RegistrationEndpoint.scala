package com.phantom.ds.registration

import com.phantom.ds.{ BasicCrypto, DataHttpService }
import com.phantom.ds.framework.httpx.PhantomJsonProtocol
import com.phantom.ds.framework.auth.EntryPointAuthenticator
import spray.http.MediaTypes._
import com.phantom.model.{ UserRegistrationRequest, RegistrationVerification }
import spray.http.StatusCodes

trait RegistrationEndpoint extends DataHttpService
    with PhantomJsonProtocol with BasicCrypto { this : EntryPointAuthenticator =>

  val registrationService = RegistrationService()
  val registration = "registration"

  val registrationRoute = pathPrefix(registration) {
    get {
      respondWithMediaType(`application/json`) {
        complete {
          StatusCodes.OK
        }
      }
    }
  }

}