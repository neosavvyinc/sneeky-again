package com.shoutout.ds.registration

import com.shoutout.ds.{ BasicCrypto, DataHttpService }
import com.shoutout.ds.framework.httpx.PhantomJsonProtocol
import com.shoutout.ds.framework.auth.EntryPointAuthenticator
import spray.http.MediaTypes._
import com.shoutout.model.{ UserRegistrationRequest, RegistrationVerification }
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