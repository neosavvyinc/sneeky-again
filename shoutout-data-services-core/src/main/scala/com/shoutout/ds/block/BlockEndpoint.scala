package com.shoutout.ds.block

import com.shoutout.ds.framework.auth.{ RequestAuthenticator, EntryPointAuthenticator }
import com.shoutout.ds.{ DataHttpService, BasicCrypto }
import com.shoutout.ds.framework.httpx.PhantomJsonProtocol

import spray.http.MediaTypes._
import spray.http.StatusCodes

/**
 * Created by aparrish on 8/14/14.
 */
trait BlockEndpoint extends DataHttpService with PhantomJsonProtocol with BasicCrypto {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  val blockService = BlockService()
  val block = "block"

  def blockContact = pathPrefix(block / IntNumber) { targetUserId =>
    authenticate(unverified _) { user =>
      post {
        respondWithMediaType(`application/json`) {
          complete {
            blockService.blockUserById(user, targetUserId)
          }
        }
      }
    }
  }

  def resetBlockList = pathPrefix(block / "reset") {
    authenticate(unverified _) { user =>
      delete {
        respondWithMediaType(`application/json`) {
          complete {
            blockService.resetBlockList(user)
          }
        }
      }
    }
  }

  val blockRoute = blockContact ~ resetBlockList

}
