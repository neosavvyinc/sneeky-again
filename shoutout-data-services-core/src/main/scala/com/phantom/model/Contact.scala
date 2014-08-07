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

case class ContactsRequest(associations : List[ContactOrdering])

case object FriendType extends ContactType

case object GroupType extends ContactType

case class AggregateContact(
  id : Long,
  sortOrder : Long,
  group : Group,
  friend : Friend,
  contactType : ContactType)

case class ContactOrdering(groupId : Option[Long],
                           friendId : Option[Long],
                           contactType : ContactType)

case class Friend(id : Option[Long])

case class Group(id : Option[Long],
                 name : String,
                 members : List[Friend])

case class Contact(id : Option[Long],
                   sortOrder : Long,
                   ownerId : Long,
                   groupId : Option[Long],
                   friendId : Option[Long],
                   contactType : ContactType = FriendType)

trait ContactComponent { this : Profile with UserComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val ContactTypeMapper = MappedTypeMapper.base[ContactType, String](ContactType.toStringRep, ContactType.fromStringRep)

  object ContactTable extends Table[Contact]("CONTACTS") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def sortOrder = column[Long]("SORT_ORDER")
    def ownerId = column[Long]("OWNER_ID")
    def contactId = column[Long]("USER_REF_ID")
    def friendId = column[Long]("USER_REF_ID")
    def groupId = column[Long]("GROUP_REF_ID")
    def contactType = column[ContactType]("CONTACT_TYPE")

    def * = id.? ~ sortOrder ~ ownerId ~ groupId.? ~ friendId.? ~ contactType <> (Contact, Contact.unapply _)
    def forInsert = * returning id

    def owner = foreignKey("OWNER_FK", ownerId, UserTable)(_.id)

  }

}
