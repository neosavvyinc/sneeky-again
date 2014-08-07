package com.phantom.ds.group

import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.BasicCrypto
import com.phantom.ds.framework.Logging
import com.phantom.model.{ GroupMembershipRequest, ShoutoutUser }

import scala.concurrent.{ ExecutionContext, Future, future }

trait GroupService {

  def createOrUpdateGroup(user : ShoutoutUser, groupMembershipRequest : GroupMembershipRequest) : Future[Int]

}

object GroupService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) = new GroupService with DatabaseSupport with Logging {

    override def createOrUpdateGroup(user : ShoutoutUser, groupMembershipRequest : GroupMembershipRequest) : Future[Int] = {

      def createGroup(user : ShoutoutUser, groupMembershipRequest : GroupMembershipRequest) : Int = {
        1
      }

      def updateGroup(user : ShoutoutUser, groupMembershipRequest : GroupMembershipRequest) : Int = {
        2
      }

      future {

        groupMembershipRequest.id match {
          case None    => createGroup(user, groupMembershipRequest)
          case Some(x) => updateGroup(user, groupMembershipRequest)
        }

      }

    }

  }

}
