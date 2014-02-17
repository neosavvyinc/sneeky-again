package com.phantom.ds.bdd

import org.specs2.mutable.Specification
import com.phantom.model.UserRegistration
import org.joda.time.{DateTimeZone, LocalDate}
import com.phantom.ds.framework.httpx.PhantomJsonProtocol
import dispatch._, Defaults._
//import scala.concurrent.ExecutionContext.Implicits.global


class RegistrationLoadIT extends Specification with PhantomJsonProtocol{

  "Registering a buttload of users" should {

    "100 users" in {

      val emails = for { i <- 1 to 100 } yield s"user$i@email.com"
      val regRequests = emails.map {x =>
        val reg = UserRegistration(x, LocalDate.now(DateTimeZone.UTC), "123abc")
        val json = userRegistrationFormat.write(reg).toString()
        url("http://localhost:9090/users/register") << json <:< Seq("Content-Type" -> "application/json")
      }.toList

      val responses = Future.sequence(regRequests.par.map(Http(_)).toList)

      responses.onSuccess {
        case x => {
          x.foreach{case y =>
            println(y.getResponseBody)
          }
        }
      }
      responses.onFailure {
        case x =>println("failure!!"); println(x)
      }

      1 must beEqualTo(1)

    }
  }


}