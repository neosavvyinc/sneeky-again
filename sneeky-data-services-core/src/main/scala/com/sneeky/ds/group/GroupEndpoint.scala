package com.sneeky.ds.group

import com.sneeky.ds.contact.ContactService
import com.sneeky.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.sneeky.ds.{ BasicCrypto, DataHttpService }
import com.sneeky.ds.framework.httpx.PhantomJsonProtocol
import com.sneeky.model.{ GroupMembershipRequest, Group, ContactsRequest, ContactOrdering }
import spray.http.MediaTypes._
import spray.http.StatusCodes

/**
 * Created by aparrish on 8/6/14.
 */
trait GroupEndpoint extends DataHttpService with PhantomJsonProtocol with BasicCrypto {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  val groupService = GroupService()
  val groups = "groups"

  def findGroups = pathPrefix(groups) {
    authenticate(unverified _) { authenticationResult =>

      val (user, sessionId) = authenticationResult

      get {
        respondWithMediaType(`application/json`) {
          complete {
            StatusCodes.OK
          }
        }
      }
    }
  }

  def createOrUpdateGroup = pathPrefix(groups) {
    authenticate(unverified _) { authenticationResult =>

      val (user, sessionId) = authenticationResult

      post {
        respondWithMediaType(`application/json`) {
          entity(as[GroupMembershipRequest]) { groupMembership =>
            complete {

              groupService.createOrUpdateGroup(user, groupMembership)

            }
          }
        }
      }
    }

  }

  val groupRoute = findGroups ~ createOrUpdateGroup

}
