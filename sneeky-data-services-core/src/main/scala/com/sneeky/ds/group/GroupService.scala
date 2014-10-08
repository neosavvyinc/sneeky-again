package com.sneeky.ds.group

import com.sneeky.dataAccess.DatabaseSupport
import com.sneeky.ds.BasicCrypto
import com.sneeky.ds.framework.Logging
import com.sneeky.ds.framework.exception.ShoutoutException
import com.sneeky.model._

import scala.concurrent.{ ExecutionContext, Future, future }
import scala.slick.session.Session

trait GroupService {

  def createOrUpdateGroup(user : ShoutoutUser, groupMembershipRequest : GroupMembershipRequest) : Future[GroupResponse]

}

object GroupService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) = new GroupService with DatabaseSupport with Logging {

    override def createOrUpdateGroup(user : ShoutoutUser, groupMembershipRequest : GroupMembershipRequest) : Future[GroupResponse] = {

      def createGroup(user : ShoutoutUser, groupMembershipRequest : GroupMembershipRequest)(implicit session : Session) : GroupResponse = {
        val group : Group = groupDao.insertGroupOperation(Group(id = groupMembershipRequest.id, ownerId = user.id.get, name = groupMembershipRequest.name))
        for (m : Int <- groupMembershipRequest.members.toSet toList) {
          shoutoutUsersDao.userExistsOperation(m) match {
            case Some(x) => if (x) { groupDao.insertGroupItemOperation(GroupItem(None, group.id.get, m)) }
            case None    => //do nothing
          }
        }
        val members = groupDao.findMembers(group.id.get)
        contactsDao.insertGroupAssociation(user, ContactOrdering(group.id, None, GroupType), contactsDao.countContactsForUser(user))
        GroupResponse(group.id.get, user.id.get, group.name, members.map {
          m => Friend(m.id, m.username, m.facebookID, m.firstName, m.lastName, m.profilePictureUrl)
        })
      }

      def updateGroup(user : ShoutoutUser, groupMembershipRequest : GroupMembershipRequest)(implicit session : Session) : GroupResponse = {

        val group = groupDao.findByIdOperation(groupMembershipRequest.id.get)

        group match {
          case None => throw ShoutoutException.groupNotFoundException
          case Some(g) => {
            groupDao.updateGroupOperation(g.copy(name = groupMembershipRequest.name))
            groupDao.deleteMembersOperation(g.id.get)

            for (m : Int <- groupMembershipRequest.members.toSet toList) {
              shoutoutUsersDao.userExistsOperation(m) match {
                case Some(x) => if (x) { groupDao.insertGroupItemOperation(GroupItem(None, g.id.get, m)) }
                case None    => //do nothing
              }
            }

            val members = groupDao.findMembers(g.id.get)
            GroupResponse(g.id.get, user.id.get, groupMembershipRequest.name, members.map {
              m => Friend(m.id, m.username, m.facebookID, m.firstName, m.lastName, m.profilePictureUrl)
            })
          }
        }

      }

      future {

        db.withTransaction { implicit session =>

          groupMembershipRequest.id match {
            case None    => createGroup(user, groupMembershipRequest)
            case Some(x) => updateGroup(user, groupMembershipRequest)
          }

        }

      }

    }

  }

}
