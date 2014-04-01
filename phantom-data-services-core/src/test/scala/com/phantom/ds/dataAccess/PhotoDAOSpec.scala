package com.phantom.ds.dataAccess

import com.phantom.model.{ Photo, PhotoList, PhotoCategory }

class PhotoDAOSpec extends BaseDAOSpec {

  sequential

  "PhotoDAO" should {
    "support retrieving a list of stock photos sorted by category" in withSetupTeardown {
      insertTestPhotoCategories()
      insertTestPhotos()
      val photosAndCategories = photoDao.findAll
      photosAndCategories.length must equalTo(2)
      photosAndCategories.head.images.length must equalTo(1)
      photosAndCategories.tail.head.images.length must equalTo(2)
    }

  }
}
