package com.shoutout.ds

import spray.routing.HttpService
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import spray.httpx.SprayJsonSupport
import com.shoutout.ds.framework.httpx.PhantomResponseMarshaller
import com.shoutout.ds.framework.Logging

trait DataHttpService extends HttpService
    with PhantomResponseMarshaller
    with SprayJsonSupport
    with Logging {

  //for now
  implicit def ec : ExecutionContext = global

}