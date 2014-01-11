package com.phantom.ds.dataAccess

import org.specs2.mutable.Specification
import com.phantom.dataAccess.DatabaseSupport
import org.specs2.specification.BeforeAfter
import com.phantom.model.{ Conversation, PhantomUser, ConversationItem }
import org.joda.time.LocalDate

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 9:34 PM
 * To change this template use File | Settings | File Templates.
 */
class BaseDAOSpec extends Specification with DatabaseSupport {

  object withSetupTeardown extends BeforeAfter {
    def before {
      println("Executing before stuff...")
      conversations.dropDB
      conversations.createDB

    }

    def after {
      println("Executing after astuff...")
      //      conversations.dropDB
    }
  }

  def setupConversationItems(convId : Long) : List[ConversationItem] = {
    val item1 = new ConversationItem(
      None, convId, "image1Url", "image1Text"
    )
    val item2 = new ConversationItem(
      None, convId, "image1Url", "image1Text"
    )
    val item3 = new ConversationItem(
      None, convId, "image1Url", "image1Text"
    )
    List(item1, item2, item3)
  }

  def insertTestUsers {
    val user1 = new PhantomUser(None, "aparrish@neosavvy.com", new LocalDate(1981, 8, 10), true, "1234567")
    val user2 = new PhantomUser(None, "ccaplinger@neosavvy.com", new LocalDate(1986, 10, 12), true, "1234567")
    val user3 = new PhantomUser(None, "tewen@neosavvy.com", new LocalDate(1987, 8, 16), true, "1234567")
    val user4 = new PhantomUser(None, "dhamlettneosavvy.com", new LocalDate(1985, 5, 17), true, "1234567")
    val user5 = new PhantomUser(None, "nick.sauro@gmail.com", new LocalDate(1987, 8, 16), true, "1234567")
    val user6 = new PhantomUser(None, "pablo.alonso@gmail.com", new LocalDate(1987, 8, 16), true, "1234567")
    phantomUsers.insert(user1)
    phantomUsers.insert(user2)
    phantomUsers.insert(user3)
    phantomUsers.insert(user4)
    phantomUsers.insert(user5)
    phantomUsers.insert(user6)
  }

  def insertTestConversations {

    val conv1 = new Conversation(None, 1, 2)
    val conv2 = new Conversation(None, 3, 4)
    val conv3 = new Conversation(None, 5, 6)
    conversations.insert(conv1)
    conversations.insert(conv2)
    conversations.insert(conv3)

  }

  def insertTestUsersAndConversations {
    insertTestUsers
    insertTestConversations
  }
}
