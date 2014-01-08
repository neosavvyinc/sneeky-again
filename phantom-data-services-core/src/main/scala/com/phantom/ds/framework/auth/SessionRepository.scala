package com.phantom.ds.framework.auth

import com.phantom.model.PhantomUser
import scala.concurrent.{ ExecutionContext, Future }
import org.joda.time.{ DateTimeZone, LocalDate }

trait SessionRepository {

  def getUser(sessionId : String) : Option[PhantomUser]

}

trait SqlSessionRepository extends SessionRepository {

  def getUser(sessionId : String) : Option[PhantomUser] = ???

}

trait MockSessionRepository extends SessionRepository {

  def getUser(sessionId : String) : Option[PhantomUser] = Some(PhantomUser(None, "nsauro@sauron.com", new LocalDate(2003, 12, 21), true, ""))

}
