package com.cacoveanu.reader.service

import org.junit.jupiter.api.Test

class ImageResize {

  @Test def comicServiceShouldLoadImageBytes() {
    val path = "C:\\Users\\silvi\\Dropbox\\comics\\Avatar The Legend Of Korra\\The Legend of Korra - Turf Wars (001-003)(2017-2018)(digital)(Raven)\\The Legend of Korra - Turf Wars - Part 1 (2017) (Digital) (Raven).cbz"
    val comicService = new ComicService
    val page = comicService.readPage(path, 0)

    assert(page.isDefined)
    assert(page.get.data.length > 0)
    println("page length: " + page.get.data.length)
  }
}
