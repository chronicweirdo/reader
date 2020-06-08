package com.cacoveanu.reader.service

object MetadataLoadTest {

  def main(args: Array[String]): Unit = {
    val service = new ComicService()
    service.comicsLocation = "d:\\comics"
    service.imageService = new ImageService()

    val file = "d:\\comics\\Astro City\\2013 New 52\\Astro City 03.cbz"
    println("title: " + service.getComicTitle(file))
    println("collection: " + service.getComicCollection(file))
    println("pages: " + service.countPagesFromDisk(file))
    println("cover: " + service.readCover(file))
  }
}
