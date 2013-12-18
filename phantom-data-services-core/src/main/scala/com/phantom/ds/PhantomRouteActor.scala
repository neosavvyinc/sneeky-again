package com.phantom.ds

import akka.actor.Actor
import com.phantom.ds.user.UserEndpoint
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.phantom.ds.conversation.ConversationEndpoint

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 11/16/13
 * Time: 4:53 PM
 */

class PhantomRouteActor() extends Actor with UserEndpoint with ConversationEndpoint {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(
    userRoute ~ conversationRoute
  )
}
