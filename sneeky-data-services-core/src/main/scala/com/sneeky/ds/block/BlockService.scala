package com.sneeky.ds.block

import com.sneeky.dataAccess.DatabaseSupport
import com.sneeky.ds.BasicCrypto
import com.sneeky.ds.framework.Logging
import com.sneeky.model.ShoutoutUser

import scala.concurrent.{ Future, ExecutionContext, future }

/**
 * Created by aparrish on 8/14/14.
 */
trait BlockService {

  def blockUserById(owner : ShoutoutUser, targetId : Long) : Future[Boolean]
  def resetBlockList(owner : ShoutoutUser) : Future[Int]
}

object BlockService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) = new BlockService with DatabaseSupport with Logging {

    override def blockUserById(owner : ShoutoutUser, targetId : Long) : Future[Boolean] = {
      future {
        val blockReference = blockUserDao.findBlockByOwnerAndTargetId(owner.id.get, targetId)
        blockReference match {
          case None => {
            blockUserDao.insertBlock(owner.id.get, targetId) match {
              case None    => false
              case Some(_) => true
            }
          }
          case Some(b) => true
        }
      }
    }

    override def resetBlockList(owner : ShoutoutUser) : Future[Int] = {
      future {
        blockUserDao.clearBlocksForOwner(owner.id.get)
      }
    }

  }

}
