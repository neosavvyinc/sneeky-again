package com.phantom.ds.contact

import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.BasicCrypto
import com.phantom.ds.framework.Logging
import com.phantom.ds.framework.exception.ShoutoutException
import com.phantom.model._

import scala.concurrent.{ Future, future, ExecutionContext }
import scala.slick.session.Session

/**
 * Created by aparrish on 8/6/14.
 */
trait ContactService {

  def saveContacts(user : ShoutoutUser, contactsRequest : ContactsRequest) : Future[Int]
  def findContacts(user : ShoutoutUser) : Future[List[AggregateContact]]
  def addContact(user : ShoutoutUser, request : ContactByUsernameRequest) : Future[Int]

}

object ContactService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) = new ContactService with DatabaseSupport with Logging {

    def addContact(user : ShoutoutUser, request : ContactByUsernameRequest) : Future[Int] = {
      future {
        db.withTransaction { implicit s =>
          //find the user to see if they exist
          shoutoutUsersDao.findByUsernameOperation(request.username) match {
            case None => throw ShoutoutException.nonExistentUser
            case Some(persistentUser) => {
              contactsDao.findContactByUsernameForOwner(user, request.username) match {
                case None => {
                  val c = ContactOrdering(None, persistentUser.id, FriendType)
                  contactsDao.insertFriendAssociation(user, c, None)
                  1 //?
                }
                case Some((contact, user)) => {
                  0 //?
                }

              }
            }
          }
        }
      }
    }

    def saveContacts(user : ShoutoutUser, contactsRequest : ContactsRequest) : Future[Int] = {

      def saveAssociationStep(user : ShoutoutUser, contactObjects : List[ContactOrdering], acc : Int)(implicit session : Session) : Int = {
        contactObjects match {
          case Nil => acc
          case h :: t => {
            h.contactType match {
              case FriendType => contactsDao.insertFriendAssociation(user, h, Some(acc))
              case GroupType  => contactsDao.insertGroupAssociation(user, h, Some(acc))
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

      def findFriendById(id : Option[Long])(implicit s : Session) : Option[Friend] = id match {
        case None => None
        case Some(x) => {
          val userOpt : Option[ShoutoutUser] = for {
            user <- shoutoutUsersDao.findByIdOperation(x)
          } yield user

          userOpt match {
            case None => None
            case Some(u) => Some(Friend(
              u.id,
              u.username,
              u.firstName,
              u.lastName,
              u.profilePictureUrl))
          }
        }

      }

      def findGroupById(id : Option[Long])(implicit s : Session) : Option[GroupResponse] = id match {
        case None => None
        case Some(x) => {
          val group = for {
            g <- groupDao.findByIdOperation(x)
          } yield g

          group match {
            case None => None
            case Some(group) => {
              val members = groupDao.findMembers(group.id.get)
              val friends = members.map {
                m =>
                  Friend(m.id,
                    m.username,
                    m.firstName,
                    m.lastName,
                    m.profilePictureUrl)
              }
              val groupResponse = GroupResponse(group.id.get, group.ownerId, group.name, friends)

              Some(groupResponse)
            }
          }

        }

      }

      future {
        db.withSession { implicit s =>

          val aggregate = contactsDao.findAllForUser(user).map(
            c => c.contactType match {
              case FriendType => AggregateContact(c.sortOrder, None, findFriendById(c.friendId), c.contactType)
              case GroupType  => AggregateContact(c.sortOrder, findGroupById(c.groupId), None, c.contactType)
            }
          )
          aggregate

        }
      }

    }

  }

}