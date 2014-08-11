package com.phantom.ds

import akka.actor.{ ActorRef, Actor }
import com.phantom.ds.contact.ContactEndpoint
import com.phantom.ds.group.GroupEndpoint
import com.phantom.ds.user.UserEndpoint
import com.phantom.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.phantom.ds.conversation.ShoutoutEndpoint
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.registration.RegistrationEndpoint
import com.phantom.ds.integration.amazon.S3Service

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 11/16/13
 * Time: 4:53 PM
 */

class ShoutoutRouteActor(val appleActor : ActorRef, val s3Service : S3Service) extends Actor
    with UserEndpoint
    with ContactEndpoint
    with RegistrationEndpoint
    with ShoutoutEndpoint
    with GroupEndpoint
    with DatabaseSupport {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(
    userRoute ~ shoutoutRoute ~ registrationRoute ~ contactRoute ~ groupRoute
  )
}
