package com.phantom.ds

import akka.actor.{ ActorRef, Actor }
import com.phantom.ds.user.UserEndpoint
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.phantom.ds.conversation.ConversationEndpoint
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.integration.twilio.TwilioEndpoint
import com.phantom.ds.registration.RegistrationEndpoint

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 11/16/13
 * Time: 4:53 PM
 */

class PhantomRouteActor(val twilioActor : ActorRef) extends Actor
    with UserEndpoint
    with RegistrationEndpoint
    with ConversationEndpoint
    with TwilioEndpoint
    with DatabaseSupport {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(
    userRoute ~ conversationRoute ~ twilioRoute ~ registrationRoute
  )
}
