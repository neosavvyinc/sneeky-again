package com.phantom.ds.conversation

import scala.concurrent.{ Future, ExecutionContext, future }
import com.phantom.model._
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.{ BasicCrypto, DSConfiguration }
import com.phantom.model.BlockUserByConversationResponse
import com.phantom.model.ConversationUpdateResponse
import com.phantom.model.Conversation
import com.phantom.model.ConversationItem
import com.phantom.model.ConversationInsertResponse
import com.phantom.ds.framework.{ Dates, Logging }
import akka.actor.ActorRef
import com.phantom.ds.integration.apple.AppleNotification
import com.phantom.ds.framework.exception.PhantomException
import scala.slick.session.Session
import java.util.UUID

import com.phantom.ds.integration.amazon.S3Service

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:01 PM
 */
trait ConversationService {

  def hello() : String

}

object ConversationService extends DSConfiguration with BasicCrypto {

  def apply(appleActor : ActorRef, s3Service : S3Service)(implicit ec : ExecutionContext) =
    new ConversationService with DatabaseSupport with Logging {

      def hello() : String = {
        "Hello"
      }

    }
}
