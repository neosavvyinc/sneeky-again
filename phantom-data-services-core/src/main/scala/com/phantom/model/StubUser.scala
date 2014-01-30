package com.phantom.model

import com.phantom.dataAccess.Profile

case class StubUser(id : Option[Long], phoneNumber : String, invitationCount : Int) extends Phantom

case class StubConversation(id : Option[Long], fromUser : Long, toStubUser : Long, imageText : String, imageUrl : String)

trait StubUserComponent { this : Profile with ConversationComponent =>

  import profile.simple._

  object StubUserTable extends Table[StubUser]("STUB_USERS") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def phoneNumber = column[String]("PHONE_NUMBER")
    def invitationCount = column[Int]("INVITATION_COUNT")
    def * = id.? ~ phoneNumber ~ invitationCount <> (StubUser, StubUser.unapply _)
    def forInsert = * returning id
    //def phoneNumberUnique = index("phonenNumberUnique", phoneNumber, unique = true)
  }
}

trait StubConversationComponent { this : Profile with StubUserComponent with UserComponent =>

  import profile.simple._

  object StubConversationTable extends Table[StubConversation]("STUB_CONVERSATIONS") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def fromUser = column[Long]("FROM_USER")
    def toStubUser = column[Long]("TO_STUB_USER")
    def imageUrl = column[String]("IMAGE_URL")
    def imageText = column[String]("IMAGE_TEXT")
    def * = id.? ~ fromUser ~ toStubUser ~ imageText ~ imageUrl <> (StubConversation, StubConversation.unapply _)
    def forInsert = * returning id
    //these break the slick generated ddl :-/
    //def fromUserFK = foreignKey("FROM_USER_FK", fromUser, UserTable)(_.id)
    //def toStubUserFK = foreignKey("TO_STUB_USER_FK", toStubUser, StubUserTable)(_.id)
  }
}

