package com.phantom.ds

import spray.routing.HttpService
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import spray.httpx.SprayJsonSupport
import com.phantom.ds.framework.httpx.PhantomResponseMarshaller
import com.phantom.ds.framework.Logging

trait DataHttpService extends HttpService
    with PhantomResponseMarshaller
    with SprayJsonSupport
    with Logging {

  //for now
  implicit def ec : ExecutionContext = global

}