package com.currant.ds

import spray.routing.HttpService
import com.currant.ds.db.DB
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import spray.httpx.SprayJsonSupport

trait DataHttpService extends HttpService with SprayJsonSupport {

  def db : DB

  //for now
  implicit def ec : ExecutionContext = global

}