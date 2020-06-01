package com.cacoveanu.reader.util

import java.io.File
import java.nio.file.Paths

import scala.collection.mutable

object FileUtil {

  def getExtension(path: String): String = {
    val lastDotIndex = path.lastIndexOf('.')
    if (lastDotIndex >= 0) path.toLowerCase().substring(lastDotIndex).substring(1)
    else ""
  }

  def getFileName(path: String): String = {
    val pathObject = Paths.get(path);
    val fileName = pathObject.getFileName.toString
    val lastDotIndex = fileName.lastIndexOf('.')
    if (lastDotIndex >= 0) fileName.substring(0, fileName.lastIndexOf('.'))
    else fileName
  }

  def scan(path: String): Seq[File] = {
    var files = mutable.Seq[File]()
    files = files :+ new File(path)
    var processed = 0
    while (processed < files.length) {
      val current = files(processed)
      if (current.exists() && current.isDirectory()) {
        files ++= current.listFiles
      }
      processed += 1
    }

    files.toSeq
  }

  def scanFilesRegex(path: String, regex: String): Seq[String] = {
    val pattern = regex.r
    scan(path)
      .filter(f => f.isFile)
      .filter(f => pattern.pattern.matcher(f.getAbsolutePath).matches)
      .map(f => f.getAbsolutePath)
  }
}
