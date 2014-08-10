package com.phantom.model

import com.phantom.dataAccess.Profile
import org.joda.time.LocalDate
import java.sql.Date

sealed trait ContactType

object ContactType {

  def toStringRep(contactType : ContactType) : String = contactType match {
    case FriendType => "friend"
    case GroupType  => "group"
  }

  def fromStringRep(contactType : String) : ContactType = contactType.toLowerCase match {
    case "friend" => FriendType
    case "group"  => GroupType
    case _        => throw new Exception(s"unrecognized ContactType $contactType")

  }

}

case class ContactByUsernameRequest(username : String)

case class ContactByFacebookIdsRequest(facebookIds : List[String])

case class DeleteContactRequest(contactType : ContactType,
                                friendRefId : Option[Long],
                                groupRefId : Option[Long])

case class ContactsRequest(associations : List[ContactOrdering])

case object FriendType extends ContactType

case object GroupType extends ContactType

case class AggregateContact(
  sortOrder : Int,
  group : Option[GroupResponse],
  friend : Option[Friend],
  contactType : ContactType)

case class ContactOrdering(groupId : Option[Long],
                           friendId : Option[Long],
                           contactType : ContactType)

case class Friend(id : Option[Long],
                  username : String,
                  facebookId : Option[String],
                  firstName : Option[String],
                  lastName : Option[String],
                  profilePictureUrl : Option[String])

case class Contact(id : Option[Long],
                   sortOrder : Int,
                   ownerId : Long,
                   groupId : Option[Long],
                   friendId : Option[Long],
                   contactType : ContactType = FriendType)

case class GroupMembershipRequest(id : Option[Long],
                                  name : String,
                                  members : List[Int])

trait ContactComponent { this : Profile with UserComponent with GroupComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val ContactTypeMapper = MappedTypeMapper.base[ContactType, String](ContactType.toStringRep, ContactType.fromStringRep)

  object ContactTable extends Table[Contact]("CONTACTS") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def sortOrder = column[Int]("SORT_ORDER", O.Nullable)
    def ownerId = column[Long]("OWNER_ID")
    def friendId = column[Long]("USER_REF_ID")

    def groupId = column[Long]("GROUP_REF_ID")
    def contactType = column[ContactType]("CONTACT_TYPE")

    def * = id.? ~ sortOrder ~ ownerId ~ groupId.? ~ friendId.? ~ contactType <> (Contact, Contact.unapply _)
    def forInsert = * returning id

    def owner = foreignKey("OWNER_FK", ownerId, UserTable)(_.id)
    def friend = foreignKey("FRIEND_FK", friendId, UserTable)(_.id)
    def group = foreignKey("GROUP_FK", groupId, GroupTable)(_.id)

  }

}
