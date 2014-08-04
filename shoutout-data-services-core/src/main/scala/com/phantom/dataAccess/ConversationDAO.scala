package com.phantom.dataAccess

import scala.slick.session.Database
import com.phantom.model.{ ConversationItem, Conversation }
import scala.concurrent.{ Future, ExecutionContext, future }
import com.phantom.ds.framework.{ Dates, Logging }
import com.phantom.ds.framework.exception.PhantomException

class ConversationDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext)
    extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

}

