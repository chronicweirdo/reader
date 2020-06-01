package com.cacoveanu.reader.util

object FileUtil {

  def getExtension(path: String) = {
    val lastDotIndex = path.lastIndexOf('.')
    if (lastDotIndex >= 0) path.toLowerCase().substring(lastDotIndex).substring(1)
    else ""
  }
}
