package com.cacoveanu.reader.controller

import java.util.Base64

import com.cacoveanu.reader.service.ComicService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
class ComicController @Autowired() (private val comicService: ComicService) {

  @RequestMapping(Array("/collection"))
  def getComicCollection(): String = {
    val path = "C:\\Users\\silvi\\Dropbox\\comics\\Avatar The Legend Of Korra\\The Legend of Korra - Turf Wars (001-003)(2017-2018)(digital)(Raven)";
    val comics = comicService.loadComicFiles(path);

    val page = new StringBuilder()
    page.append("<html><body>")
    for (comic <- comics) {
      page.append("<p>")
      page.append(comic.title)
      val coverEncoded = new String(Base64.getEncoder().encode(comic.cover.data))
      page.append("<img style=\"max-width: 100px;\" src=\"data:" + comic.cover.mediaType + ";base64,")
      page.append(coverEncoded)
      page.append("\">")
      page.append("</p>")
    }
    page.append("</body></html>")

    page.toString()
  }
}
