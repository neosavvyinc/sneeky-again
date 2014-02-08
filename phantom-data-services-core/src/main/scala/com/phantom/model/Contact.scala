package com.phantom.model

import com.phantom.dataAccess.Profile
import org.joda.time.LocalDate
import java.sql.Date

sealed trait ContactType

object ContactType {

  def toStringRep(contactType : ContactType) : String = contactType match {
    case Friend  => "friend"
    case Blocked => "blocked"
  }

  def fromStringRep(contactType : String) : ContactType = contactType.toLowerCase match {
    case "friend"  => Friend
    case "blocked" => Blocked
    case _         => throw new Exception(s"unrecognized ContactType $contactType")

  }

}

case object Friend extends ContactType

case object Blocked extends ContactType

case class Contact(id : Option[Long],
                   ownerId : Long,
                   contactId : Long,
                   contactType : ContactType = Friend)

trait ContactComponent { this : Profile with UserComponent =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  implicit val ContactTypeMapper = MappedTypeMapper.base[ContactType, String](ContactType.toStringRep, ContactType.fromStringRep)

  object ContactTable extends Table[Contact]("CONTACTS") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    // * UNIQUE * //
    def ownerId = column[Long]("OWNER_ID")

    // * UNIQUE * //
    def contactId = column[Long]("CONTACT_ID")
    def contactType = column[ContactType]("TYPE")
    def * = id.? ~ ownerId ~ contactId ~ contactType <> (Contact, Contact.unapply _)
    def forInsert = * returning id

    def owner = foreignKey("OWNER_FK", ownerId, UserTable)(_.id)
    def contact = foreignKey("CONTACT_FK", contactId, UserTable)(_.id)
    def uniqueContact = index("uniqueContact", (ownerId, contactId), unique = true)
  }

}
