package com.phantom.ds.group

import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.BasicCrypto
import com.phantom.ds.framework.Logging

import scala.concurrent.ExecutionContext

trait GroupService {

}

object GroupService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) = new GroupService with DatabaseSupport with Logging {

  }

}
