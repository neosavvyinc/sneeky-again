package com.phantom.ds.dataAccess

import org.specs2.mutable.Specification
import com.phantom.dataAccess.DatabaseSupport
import org.specs2.specification.BeforeAfter
import com.phantom.model._
import org.joda.time.{ DateTimeZone, LocalDate }
import java.util.UUID
import com.phantom.ds.user.Passwords
import com.phantom.ds.TestUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.session.Session
import com.phantom.ds.conversation.FeedFolder

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 9:34 PM
 * To change this template use File | Settings | File Templates.
 */
trait BaseDAOSpec extends Specification with DatabaseSupport with TestUtils {

  object withSetupTeardown extends BeforeAfter {
    def before {
      dataAccessLayer.drop(db)
      dataAccessLayer.create(db)
    }

    def after {
      dataAccessLayer.drop(db)
    }
  }

  //override def after : Any = source.close _

  def setupConversationItems(convId : Long, toUser : Long, fromUser : Long) : List[ConversationItem] = {
    val item1 = new ConversationItem(
      None, convId, "image1Url", "image1Text", toUser, fromUser
    )
    val item2 = new ConversationItem(
      None, convId, "image1Url", "image1Text", toUser, fromUser
    )
    val item3 = new ConversationItem(
      None, convId, "image1Url", "image1Text", toUser, fromUser
    )
    List(item1, item2, item3)
  }

  def createVerifiedUser(email : String, password : String, phoneNumber : String = "") : PhantomUser = {
    val user = PhantomUser(None, UUID.randomUUID, Some(email), Some(Passwords.getSaltedHash(password)), Some(LocalDate.now(DateTimeZone.UTC)), true, Some(phoneNumber), Verified)
    phantomUsersDao.insert(user)
  }

  def createUnverifiedUser(email : String, password : String, phoneNumber : Option[String] = None) = {
    phantomUsersDao.insert(PhantomUser(None, UUID.randomUUID, Some(email), Some(Passwords.getSaltedHash(password)), Some(LocalDate.now(DateTimeZone.UTC)), true, phoneNumber, Unverified))
  }

  def createStubUser(phone : String, count : Int = 1) = {
    phantomUsersDao.insert(PhantomUser(None, UUID.randomUUID, None, None, Some(LocalDate.now(DateTimeZone.UTC)), true, Some(phone), Stub, count))
  }

  def createConversation(fromId : Long, toId : Long) : Conversation = {
    conversationDao.insert(Conversation(None, toId, fromId, "9197419597"))
  }

  def insertTestUsers() {
    createVerifiedUser("aparrish@neosavvy.com", "password", "111111")
    createVerifiedUser("ccaplinger@neosavvy.com", "password", "222222")
    createVerifiedUser("tewen@neosavvy.com", "password", "333333")
    createVerifiedUser("dhamlett@neosavvy.com", "password", "444444")
    createVerifiedUser("nick.sauro@gmail.com", "password", "555555")
    createVerifiedUser("pablo.alonso@gmail.com", "password", "666666")
  }

  def insertTestPhotoCategories() {
    val c1 = PhotoCategory(None, "backgrounds")
    val c2 = PhotoCategory(None, "meems")
    photoDao.insertCategory(c1)
    photoDao.insertCategory(c2)
  }

  def insertTestPhotos() {
    val p1 = Photo(None, 1, true, "/somewhere/1")
    val p2 = Photo(None, 2, true, "/somewhere/2")
    val p3 = Photo(None, 2, true, "/somewhere/3")
    photoDao.insertPhoto(p1)
    photoDao.insertPhoto(p2)
    photoDao.insertPhoto(p3)
  }

  def insertUsersWithPhoneNumbersAndContacts() = {
    insertTestUsers()
    insertTestContacts()
  }

  def insertTestContacts() {
    contacts.insertAll(
      Seq(
        Contact(None, 1, 2),
        Contact(None, 1, 3),
        Contact(None, 1, 4)
      )
    )
  }

  def insertTestConversations() {

    val conv1 = new Conversation(None, 1, 2, "9197419597")
    val conv2 = new Conversation(None, 3, 4, "9197419597")
    val conv3 = new Conversation(None, 5, 6, "9197419597")
    conversationDao.insert(conv1)
    conversationDao.insert(conv2)
    conversationDao.insert(conv3)

  }

  def getUser(verificationId : UUID) : PhantomUser = {
    val user = phantomUsersDao.findByUUID(verificationId);
    user.get
  }

  def insertTestConverationsWithItems() {
    insertTestUsersAndConversations()

    val conv1item1 = new ConversationItem(None, 1, "imageUrl1", "imageText1", 1, 2)
    val conv1item2 = new ConversationItem(None, 1, "imageUrl2", "imageText2", 1, 2)
    val conv1item3 = new ConversationItem(None, 1, "imageUrl3", "imageText3", 1, 2)

    val conv2item1 = new ConversationItem(None, 2, "imageUrl1", "imageText1", 3, 4)
    val conv2item2 = new ConversationItem(None, 2, "imageUrl2", "imageText2", 3, 4)
    val conv2item3 = new ConversationItem(None, 2, "imageUrl3", "imageText3", 3, 4)

    await(conversationItemDao.insertAll(Seq(conv1item1, conv1item2, conv1item3, conv2item1, conv2item2, conv2item3)))
  }

  def insertTestUsersAndConversations() {
    insertTestUsers()
    insertTestConversations()
  }

  def getFullFeed(id : Long) = db.withSession {
    session : Session =>
      FeedFolder.foldFeed(id, conversationDao.findConversationsAndItemsOperation(id)(session), NoPaging)
  }

}
