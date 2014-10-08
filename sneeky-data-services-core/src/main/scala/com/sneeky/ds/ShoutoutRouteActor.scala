package com.sneeky.ds

import akka.actor.{ ActorRef, Actor }
import com.sneeky.ds.block.BlockEndpoint
import com.sneeky.ds.contact.ContactEndpoint
import com.sneeky.ds.group.GroupEndpoint
import com.sneeky.ds.health.HealthCheckEndpoint
import com.sneeky.ds.stats.StatsEndpoint
import com.sneeky.ds.user.UserEndpoint
import com.sneeky.ds.framework.auth.{ EntryPointAuthenticator, RequestAuthenticator }
import com.sneeky.ds.shoutout.ShoutoutEndpoint
import com.sneeky.dataAccess.DatabaseSupport
import com.sneeky.ds.integration.amazon.S3Service

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
    with HealthCheckEndpoint
    with DatabaseSupport
    with StatsEndpoint {

  this : RequestAuthenticator with EntryPointAuthenticator =>

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(userRoute ~ shoutoutRoute ~ contactRoute ~ groupRoute ~ blockRoute ~ healthCheckRoute ~ statsRoute)

}
