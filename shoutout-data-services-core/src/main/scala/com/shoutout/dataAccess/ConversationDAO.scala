package com.shoutout.dataAccess

import scala.slick.session.Database
import com.shoutout.model.{ ConversationItem, Conversation }
import scala.concurrent.{ Future, ExecutionContext, future }
import com.shoutout.ds.framework.{ Dates, Logging }
import com.shoutout.ds.framework.exception.ShoutoutException

class ConversationDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext)
    extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

}

