package com.phantom.ds

import spray.routing.HttpService
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import spray.httpx.SprayJsonSupport

trait DataHttpService extends HttpService with SprayJsonSupport {

  //for now
  implicit def ec : ExecutionContext = global

}