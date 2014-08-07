package com.phantom.ds.group

import com.phantom.ds.contact.ContactService
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.phantom.ds.{ BasicCrypto, DataHttpService }
import com.phantom.ds.framework.httpx.PhantomJsonProtocol
import com.phantom.model.{ GroupMembershipRequest, Group, ContactsRequest, ContactOrdering }
import spray.http.MediaTypes._
import spray.http.StatusCodes

/**
 * Created by aparrish on 8/7/14.
 */
/**
 * Created by aparrish on 8/6/14.
 */
trait GroupEndpoint extends DataHttpService with PhantomJsonProtocol with BasicCrypto {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  val groupService = GroupService()
  val groups = "groups"

  def findGroups = pathPrefix(groups) {
    authenticate(unverified _) { user =>
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
    authenticate(unverified _) { user =>
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
