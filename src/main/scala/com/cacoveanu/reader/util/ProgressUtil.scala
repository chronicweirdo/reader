package com.cacoveanu.reader.util

import com.cacoveanu.reader.entity.{Book, Progress}

object ProgressUtil {
  def fixProgressForBook(oldProgress: Progress, newBook: Book): Progress = {
    if (oldProgress.size != newBook.size) {
      val validPosition = Math.floor(oldProgress.position.toDouble/oldProgress.size * newBook.size).toInt
      new Progress(oldProgress.username, newBook, validPosition, oldProgress.lastUpdate, oldProgress.finished)
    } else {
      // nothing to fix
      new Progress(oldProgress.username, newBook, oldProgress.position, oldProgress.lastUpdate, oldProgress.finished)
    }
  }
}
