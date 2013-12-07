package com.currant.ds.profile

import com.currant.model._
import com.currant.ds.db.DB
import scala.concurrent.ExecutionContext
import com.currant.model.Profile
import com.currant.model.ExtendedProfile

trait ProfileService {

  def findProfile(id : Float) : Profile
  def findExtendedProfile(id : Float) : ExtendedProfile

}

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 11/21/13
 * Time: 8:39 PM
 */
object ProfileService {

  /**
   * GET           /profiles/:id                controllers.Profiles.find(id: Float)
   * GET           /profiles/:id/extended       controllers.Profiles.findExtended(id: Float)
   * PUT           /profiles/:id                controllers.Profiles.update()
   * GET           /profiles/:id/clubs          controllers.Profiles.findClubs()
   * GET           /profiles/:id/clubs/explore  controllers.Profiles.exploreClubs()
   * POST          /profiles/:id/clubs          controllers.Profiles.updateClubs()
   * GET           /profiles/:id/friends        controllers.Profiles.findFriends(currantUserId: Float, friendStatus: String)
   * POST          /profiles/:id/friends/       controllers.Profiles.updateFriends(currantUserId: Float)
   * GET           /profiles/:id/games          controllers.Profiles.findAllGames(id: Float, location: /* other query/filter params */)
   * POST          /profiles/:id/games          controllers.Profiles.updateGames(id: Float)
   */

  def apply(db : DB)(implicit ec : ExecutionContext) = new ProfileService {
    def findProfile(id : Float) : Profile = {

      new Profile(1,
        1,
        Currant,
        "SourceId",
        "Adam",
        "Parrish",
        "/somewhere/in/the/file/system",
        "Blamo he lives",
        "Sanford",
        "NC",
        "USofA",
        Elite,
        EarlyMorning,
        true,
        false,
        false,
        false)

    }

    def findExtendedProfile(id : Float) : ExtendedProfile = ???
  }

}
