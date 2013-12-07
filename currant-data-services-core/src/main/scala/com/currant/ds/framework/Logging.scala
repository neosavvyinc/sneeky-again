package com.currant.ds.framework

import org.slf4j.LoggerFactory

trait Logging {

  val log = LoggerFactory.getLogger(this.getClass)

  def debug[T <: AnyRef](t : T) = if (log.isDebugEnabled) log.debug(t.toString)
  def info[T <: AnyRef](t : T) = if (log.isInfoEnabled) log.info(t.toString)

}