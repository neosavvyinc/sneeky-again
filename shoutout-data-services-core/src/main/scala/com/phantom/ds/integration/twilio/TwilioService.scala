package com.phantom.ds.integration.twilio

import scala.concurrent.{ ExecutionContext, Future }
import com.phantom.ds.framework.Logging
import com.phantom.ds.DSConfiguration
import com.phantom.dataAccess.DatabaseSupport
import com.twilio.sdk.resource.instance.Sms
import com.phantom.model._

trait TwilioService {
  def sendInvitationsToStubUsers(stubUsers : Seq[PhantomUser]) : Future[Seq[PhantomUser]]
}
/*
This is our first iteration of our twilio integration.  This is very simple..it just reaches out to twilio using the phone number in the
user.  Regardless of the outcome..it will update the invitationCount.  As we see how the app grows..all this will change.  The Results objecdt
contains the record of what happened, it tracks failed attempts, rejected attemps and passed attempts..so we can utilize that when the
time is right.
 */
object TwilioService {
  def apply(sender : TwilioMessageSender)(implicit ec : ExecutionContext) : TwilioService =

    new TwilioService with DatabaseSupport with Logging with DSConfiguration {

      //not sure how to handle failed known stub users.
      //blacklisted might be the only real case to look out for
      //even if the send fails..we are still incrementing the invitation count....
      def sendInvitationsToStubUsers(stubUsers : Seq[PhantomUser]) : Future[Seq[PhantomUser]] = {
        val resultsF = sender.sendInvitations(stubUsers.map(_.phoneNumber).flatten).map(x => toResults(stubUsers.zip(x)))

        for {
          results <- resultsF
          _ <- updateInvitationCount(results.passed)
        } yield results.failed
      }

      private def updateInvitationCount(stubUsers : Seq[PhantomUser]) : Future[Int] = {
        if (stubUsers.length > 0) {
          phantomUsersDao.updateInvitationCount(stubUsers)
        } else {
          Future.successful(0)
        }
      }

      private def toResults[T](seq : Seq[(T, Either[TwilioSendFail, Sms])]) : Results[T] = {
        seq.foldLeft(Results[T]()) { (acc, i) =>
          i match {
            case (x, Left(y : NonTwilioException)) => acc.copy(failed = acc.failed :+ x)
            case (x, Left(_))                      => acc.copy(twilioRejected = acc.twilioRejected :+ x)
            case (x, Right(_))                     => acc.copy(passed = acc.passed :+ x)
          }
        }
      }
    }
}

case class Results[T](failed : Seq[T] = Seq(), twilioRejected : Seq[T] = Seq(), passed : Seq[T] = Seq())