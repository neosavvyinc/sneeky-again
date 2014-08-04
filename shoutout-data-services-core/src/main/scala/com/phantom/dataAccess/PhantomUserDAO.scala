package com.phantom.dataAccess

import scala.slick.session.Database
import com.phantom.ds.framework.Logging
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import java.util.UUID
import com.phantom.ds.user.Passwords
import com.phantom.model.UserLogin
import com.phantom.model.ShoutoutUser
import com.phantom.model.UserRegistration

class PhantomUserDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

}

