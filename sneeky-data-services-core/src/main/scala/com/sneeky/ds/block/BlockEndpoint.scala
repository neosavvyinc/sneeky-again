package com.sneeky.ds.block

import com.sneeky.ds.framework.auth.{ RequestAuthenticator, EntryPointAuthenticator }
import com.sneeky.ds.{ DataHttpService, BasicCrypto }
import com.sneeky.ds.framework.httpx.PhantomJsonProtocol

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
    authenticate(unverified _) { authenticationResult =>

      val (user, sessionId) = authenticationResult

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
    authenticate(unverified _) { authenticationResult =>

      val (user, sessionId) = authenticationResult

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
