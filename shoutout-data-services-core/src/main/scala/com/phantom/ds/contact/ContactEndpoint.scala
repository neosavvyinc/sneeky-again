package com.phantom.ds.contact

import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.phantom.ds.{ BasicCrypto, DataHttpService }
import com.phantom.ds.framework.httpx.PhantomJsonProtocol
import com.phantom.model.{ ContactOrdering, ContactsRequest, UserRegistrationRequest }
import spray.http.StatusCodes
import spray.http.MediaTypes._

/**
 * Created by aparrish on 8/6/14.
 */
trait ContactEndpoint extends DataHttpService with PhantomJsonProtocol with BasicCrypto {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  val contactService = ContactService()
  val contacts = "contacts"

  def findContacts = pathPrefix(contacts) {
    authenticate(unverified _) { user =>
      get {
        respondWithMediaType(`application/json`) {
          complete {
            StatusCodes.OK
            //            contactService.findContacts(user)

          }
        }
      }
    }
  }

  def saveContacts = pathPrefix(contacts / "save") {
    authenticate(unverified _) { user =>
      post {
        respondWithMediaType(`application/json`) {
          entity(as[List[ContactOrdering]]) { contactOrdering =>
            complete {

              contactService.saveContacts(user, ContactsRequest(contactOrdering))

            }
          }
        }
      }
    }

  }

  val contactRoute = saveContacts ~ findContacts

}
