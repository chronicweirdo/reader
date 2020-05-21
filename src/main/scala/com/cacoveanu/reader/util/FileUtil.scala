package com.cacoveanu.reader.util

object FileUtil {

  def getExtension(path: String) = path.toLowerCase().substring(path.lastIndexOf('.')).substring(1)
}
