package com.shoutout.ds

import akka.actor.{ Props, ActorSystem }
import akka.io.IO
import spray.can.Http
import com.shoutout.ds.framework.auth._
import com.shoutout.ds.integration.apple.{ AppleActor, AppleService }
import com.shoutout.ds.framework.Logging
import java.util.TimeZone
import org.joda.time.DateTimeZone
import com.shoutout.ds.integration.amazon.S3Service

object Boot extends App with DSConfiguration with Logging {

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  DateTimeZone.setDefault(DateTimeZone.UTC)

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("shoutout")

  val phantomService = getActor

  implicit val executor = scala.concurrent.ExecutionContext.Implicits.global

  IO(Http) ! Http.Bind(phantomService, interface = ListenConfiguration.ipAddress, port = ListenConfiguration.port)

  private def getActor = {
    val mode = AuthConfiguration.mode
    log.info(s"---->>>STARTING APPLICATION WITH AUTHENTICATION: $mode <<<------")
    mode match {
      case FullAuthentication       => system.actorOf(Props(new ShoutoutRouteActor(appleActor, S3Service()) with PhantomRequestAuthenticator with PhantomEntryPointAuthenticator), "service")
      case NonHashingAuthentication => system.actorOf(Props(new ShoutoutRouteActor(appleActor, S3Service()) with NonHashingRequestAuthenticator with PassThroughEntryPointAuthenticator), "service")
      case NoAuthentication         => system.actorOf(Props(new ShoutoutRouteActor(appleActor, S3Service()) with PassThroughRequestAuthenticator with PassThroughEntryPointAuthenticator), "service")
      case DebugAuthentication      => system.actorOf(Props(new ShoutoutRouteActor(appleActor, S3Service()) with DebugAuthenticator with PhantomEntryPointAuthenticator), "service")
    }
  }

  private def appleActor = system.actorOf(Props(new AppleActor()), "apple")

}
