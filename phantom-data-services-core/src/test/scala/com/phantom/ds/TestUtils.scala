package com.phantom.ds

import com.phantom.model.RegistrationVerification
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

trait TestUtils {

  def reg(pre : String, uuid : String, post : String) = {
    RegistrationVerification("", "", "", "", s"$pre[$uuid]$post", 1)
  }

  def regNoBracket(pre : String, uuid : String, post : String) = {
    RegistrationVerification("", "", "", "", s"$pre $uuid $post", 1)
  }

  def await[T](f : => Future[T])(implicit ec : ExecutionContext) : T = {
    Await.result(f, 5.seconds)
  }
}