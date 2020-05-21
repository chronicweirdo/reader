package com.cacoveanu.reader.service

import org.junit.jupiter.api.Test
//import org.scalatest._
//import org.scalatest.matchers.should.Matchers

class ImageResizeSpec /*extends FlatSpec with Matchers*/ {

  @Test def comicServiceShouldLoadImageBytes() {
  //"the comic service" should "load image bytes from file" in {
    val path = "C:\\Users\\silvi\\Dropbox\\comics\\Avatar The Legend Of Korra\\The Legend of Korra - Turf Wars (001-003)(2017-2018)(digital)(Raven)\\The Legend of Korra - Turf Wars - Part 1 (2017) (Digital) (Raven).cbz"
    val comicService = new ComicService
    val page = comicService.readPage(path, 0)

    //page should not be (None)
    assert(page.isDefined)
    assert(page.get.data.length > 0)
    println("page length: " + page.get.data.length)
  }
}
