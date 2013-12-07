package com.currant.ds

object QueryReader {

  def fromFile(file : String) : Seq[String] = {
    val resource = io.Source.fromURL(getClass.getResource(file)).mkString
    resource.replaceAll("\\n", "").split(";")

  }

}