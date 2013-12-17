package com.phantom.ds.framework.auth

import com.phantom.model.User
import scala.concurrent.{ ExecutionContext, Future }

trait SessionRepository {

  def getUser(sessionId : String) : Option[User]

}

object SessionRepository {
  def apply() = new SessionRepository {
    def getUser(sessionId : String) : Option[User] = ???
  }
}

trait MockSessionRepository extends SessionRepository {

  def getUser(sessionId : String) : Option[User] = Some(User(1L, "nsauro@sauron.com", "birfday", "gross", true))

}