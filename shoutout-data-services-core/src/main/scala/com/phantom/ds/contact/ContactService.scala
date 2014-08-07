package com.phantom.ds.contact

import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.BasicCrypto
import com.phantom.ds.framework.Logging
import com.phantom.model._

import scala.concurrent.{ Future, future, ExecutionContext }
import scala.slick.session.Session

/**
 * Created by aparrish on 8/6/14.
 */
trait ContactService {

  def saveContacts(user : ShoutoutUser, contactsRequest : ContactsRequest) : Future[Int]

}

object ContactService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) = new ContactService with DatabaseSupport with Logging {

    def saveContacts(user : ShoutoutUser, contactsRequest : ContactsRequest) : Future[Int] = {

      def saveAssociationStep(user : ShoutoutUser, contactObjects : List[ContactOrdering], acc : Int)(implicit session : Session) : Int = {
        contactObjects match {
          case Nil => acc
          case h :: t => {
            h.contactType match {
              case FriendType => contactsDao.insertFriendAssociation(user, h, acc)
              case GroupType  => contactsDao.insertGroupAssociation(user, h, acc)
            }
            saveAssociationStep(user, t, acc + 1)
          }
        }
      }

      future {
        db.withTransaction { implicit s =>
          contactsDao.deleteAllAssociationsForOwner(user)
          saveAssociationStep(user, contactsRequest.associations, 0)
        }
      }
    }

    def findContacts(user : ShoutoutUser) : Future[List[AggregateContact]] = {

      def findFriendById(id : Long) : Friend = {
        Friend(
          Some(1),
          Some("Adam"),
          Some("Parrish"),
          Some("http://www.neosavvy.com/assets/neosavvy_logo_only-5163ecd544a8695c3f6ed85882a8f211.png"))
      }

      def findGroupById(id : Long) : Group = {
        Group(Some(1L), 1L, "Adams Group")
      }

      future {
        db.withSession { implicit s =>

          val aggregate = contactsDao.findAllForUser(user).map(
            c => c.contactType match {
              case FriendType => AggregateContact(c.id.get, c.sortOrder, None, Some(findFriendById(c.friendId.get)), c.contactType)
              case GroupType  => AggregateContact(c.id.get, c.sortOrder, Some(findGroupById(c.groupId.get)), None, c.contactType)
            }
          )
          aggregate

        }
      }

    }
  }

}