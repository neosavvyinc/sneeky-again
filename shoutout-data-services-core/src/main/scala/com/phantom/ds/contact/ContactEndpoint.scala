package com.phantom.ds.contact

import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.phantom.ds.{ BasicCrypto, DataHttpService }
import com.phantom.ds.framework.httpx.PhantomJsonProtocol
import com.phantom.model._
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
            contactService.findContacts(user)
          }
        }
      }
    }
  }

  def saveContacts = pathPrefix(contacts / "save") {
    authenticate(unverified _) { user =>
      post {
        respondWithMediaType(`application/json`) {
          entity(as[ContactsRequest]) { request =>
            complete {
              contactService.saveContacts(user, request)
            }
          }
        }
      }
    }

  }

  def addContactByUsername = pathPrefix(contacts / "add" / "username") {
    authenticate(unverified _) { user =>
      post {
        respondWithMediaType(`application/json`) {
          entity(as[ContactByUsernameRequest]) { request =>
            complete {
              contactService.addContactByUsername(user, request)
            }
          }
        }
      }
    }
  }

  def addContactsByFacebookId = pathPrefix(contacts / "add" / "facebook") {
    authenticate(unverified _) { user =>
      post {
        respondWithMediaType(`application/json`) {
          entity(as[ContactByFacebookIdsRequest]) { request =>
            complete {
              contactService.addContactByFacebook(user, request)
            }
          }
        }
      }
    }
  }

  def deleteContact = pathPrefix(contacts / "delete") {
    authenticate(unverified _) { user =>
      post {
        respondWithMediaType(`application/json`) {
          entity(as[DeleteContactRequest]) { request =>
            complete {
              contactService.deleteContact(user, request)
            }
          }
        }
      }
    }
  }

  val contactRoute = saveContacts ~
    findContacts ~
    addContactByUsername ~
    addContactsByFacebookId ~
    deleteContact

}
