package com.shoutout.ds.framework

import java.util.Locale

import org.apache.commons.lang.LocaleUtils

/**
 * Created by aparrish on 10/8/14.
 */
object LocaleUtilities {

  def getLocaleFromString(string : String) : Locale = {
    try {

      if (string.startsWith("zh")) {
        if (string.toLowerCase().contains("hant")) {
          Locale.TRADITIONAL_CHINESE
        } else {
          Locale.SIMPLIFIED_CHINESE
        }
      } else {
        LocaleUtils.toLocale(string)
      }

    } catch {
      case e : Exception => Locale.ENGLISH
    }
  }

  /**
   * From http://www.localeplanet.com/java/
   *
   * I can't believe these aren't built in
   */
  val SPANISH = new Locale("es")
  val HEBREW = new Locale("iw")
  val HUNGARIAN = new Locale("hu")
  val SWEDISH = new Locale("sv")

}
