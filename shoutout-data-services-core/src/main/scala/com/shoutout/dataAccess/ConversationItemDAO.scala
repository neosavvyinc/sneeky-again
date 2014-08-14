package com.shoutout.dataAccess

import scala.slick.session.Database
import com.shoutout.model.ConversationItem
import scala.concurrent.{ Future, ExecutionContext, future }

class ConversationItemDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db) {
  import dal._
  import dal.profile.simple._

}

