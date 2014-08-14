package com.shoutout.ds

import akka.actor.{ ActorRef, Actor }
import com.shoutout.ds.block.BlockEndpoint
import com.shoutout.ds.contact.ContactEndpoint
import com.shoutout.ds.group.GroupEndpoint
import com.shoutout.ds.user.UserEndpoint
import com.shoutout.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.shoutout.ds.shoutout.ShoutoutEndpoint
import com.shoutout.dataAccess.DatabaseSupport
import com.shoutout.ds.integration.amazon.S3Service

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
    with ShoutoutEndpoint
    with GroupEndpoint
    with BlockEndpoint
    with DatabaseSupport {
  this : RequestAuthenticator with EntryPointAuthenticator =>

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(
    userRoute ~ shoutoutRoute ~ contactRoute ~ groupRoute ~ blockRoute
  )
}
