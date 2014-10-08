package com.shoutout.ds

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

trait TestUtils {

  def await[T](f : => Future[T])(implicit ec : ExecutionContext) : T = {
    Await.result(f, 5.seconds)
  }
}