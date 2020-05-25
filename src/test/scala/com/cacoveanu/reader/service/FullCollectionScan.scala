package com.cacoveanu.reader.service

import org.junit.jupiter.api.Test

class FullCollectionScan {

  /*@Test
  def scanWholeCollection(): Unit = {
    val path = "C:\\Users\\silvi\\Dropbox\\comics\\"

    val imageService = new ImageService
    val comicService = new ComicService
    comicService.imageService = imageService

    try {
      comicService.loadComicFiles(path)
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        println(e.getMessage)
    }
  }*/

  @Test
  def debugProblematicComics(): Unit = {
    // scanning comic: C:\Users\silvi\Dropbox\comics\Adventure Time\Specials\Peanut-AdventureTime (FCBD 2012, RaptureStar).cbr
    val path = "C:\\Users\\silvi\\Dropbox\\comics\\Adventure Time\\Specials\\Peanut-AdventureTime (FCBD 2012, RaptureStar).cbr"
    val imageService = new ImageService
    val comicService = new ComicService
    comicService.imageService = imageService

    comicService.loadComic(path)
  }
}
