package com.cacoveanu.reader.util

object TestPathNormalize {

  private def getAbsolutePath(currentResourcePath: String, oldHref: String): String = {
    if (oldHref.startsWith("/")) oldHref
    else {
      var currentPath = currentResourcePath.split("/").dropRight(1)
      val steps = oldHref.split("/")
      steps.foreach(step => step match {
        case ".." => currentPath = currentPath.dropRight(1)
        case p => currentPath = currentPath :+ p
      })
      currentPath.mkString("/")
    }
  }

  def main(args: Array[String]): Unit = {
    println(getAbsolutePath("a/b/c", "../d"))
    println(getAbsolutePath("a/b/c", "d"))
    println(getAbsolutePath("a/b/c", "/d"))
  }
}
