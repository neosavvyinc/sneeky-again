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
  def addContactByUsername(user : ShoutoutUser, request : ContactByUsernameRequest) : Future[Int]
  def addContactByFacebook(user : ShoutoutUser, request : ContactByFacebookIdsRequest) : Future[Int]
  def deleteContact(user : ShoutoutUser, request : DeleteContactRequest) : Future[Int]

}

object ContactService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) = new ContactService with DatabaseSupport with Logging {

    def deleteContact(user : ShoutoutUser, request : DeleteContactRequest) : Future[Int] = {

      def deleteFriendContact(user : ShoutoutUser, request : DeleteContactRequest)(implicit session : Session) : Int = {
        contactsDao.deleteByFriendId(user, request.friendRefId.getOrElse(throw ShoutoutException.friendIdMissing))
      }

      def deleteGroupContact(user : ShoutoutUser, request : DeleteContactRequest)(implicit session : Session) : Int = {
        contactsDao.deleteByGroupId(user, request.groupRefId.getOrElse(throw ShoutoutException.groupIdMissing))
        groupDao.deleteMembersOperation(request.groupRefId.get)
        groupDao.deleteGroup(request.groupRefId.get)
      }

      future {
        db.withTransaction { implicit s =>
          request.contactType match {
            case FriendType => deleteFriendContact(user, request)
            case GroupType  => deleteGroupContact(user, request)
          }
        }
      }
    }

    def addContactByUsername(user : ShoutoutUser, request : ContactByUsernameRequest) : Future[Int] = {
      future {
        db.withTransaction { implicit s =>
          //find the user to see if they exist
          shoutoutUsersDao.findByUsernameOperation(request.username) match {
            case None => throw ShoutoutException.contactNotUpdated
            case Some(persistentUser) => {
              contactsDao.findContactByUsernameForOwner(user, request.username) match {
                case None => {
                  val c = ContactOrdering(None, persistentUser.id, FriendType)
                  contactsDao.insertFriendAssociation(user, c, Some(contactsDao.countContactsForUser(user)))
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

    def addContactByFacebook(user : ShoutoutUser, request : ContactByFacebookIdsRequest) : Future[Int] = {

      def insertOrUpdateEachUser(users : List[ShoutoutUser], owner : ShoutoutUser, acc : Int)(implicit session : Session) : Int = {

        users match {
          case Nil => acc
          case h :: tail => {

            val contact = contactsDao.findContactByFacebookIdForOwner(owner, h.facebookID.get)
            contact match {
              case None => {
                //insert the contact
                contactsDao.insertFriendAssociation(user, ContactOrdering(None, h.id, FriendType), Some(contactsDao.countContactsForUser(user)))

                //recurse - with accumulation
                insertOrUpdateEachUser(tail, owner, acc + 1)
              }
              case Some(contact) => {
                //do not insert a new contact - it already exists

                //recurse - without accumulating
                insertOrUpdateEachUser(tail, owner, acc)
              }
            }
          }
        }

      }

      future {
        db.withTransaction { implicit s =>
          //find the user to see if they exist
          shoutoutUsersDao.findByFacebookIds(request.facebookIds) match {
            case Nil           => throw ShoutoutException.contactNotUpdated
            case facebookUsers => insertOrUpdateEachUser(facebookUsers, user, 0)
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
              u.facebookID,
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
                    m.facebookID,
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