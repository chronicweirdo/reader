package com.cacoveanu.reader.controller

import java.net.URLEncoder
import java.util.Base64

import com.cacoveanu.reader.service.ComicService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, RestController}

@RestController
class ComicController @Autowired() (private val comicService: ComicService) {

  private def getImageTag(title: String, mediaType: MediaType, image: Array[Byte], maxWidth: Option[Int]) = {
    val img = new StringBuilder()
    img.append("<img ")
    if (maxWidth.isDefined) {
      img.append("style=\"max-width: ").append(maxWidth.get).append("px\" ")
    }
    img.append("title=\"").append(title).append("\" ")
    val imageEncoded = new String(Base64.getEncoder().encode(image))
    img.append("src=\"data:").append(mediaType).append(";base64,")
      .append(imageEncoded).append("\">")
    img.toString()
  }

  @RequestMapping(Array("/collection"))
  def getComicCollection(): String = {
    //val path = "C:\\Users\\silvi\\Dropbox\\comics\\Avatar The Legend Of Korra\\The Legend of Korra - Turf Wars (001-003)(2017-2018)(digital)(Raven)";
    val path = "C:\\Users\\silvi\\Dropbox\\comics\\"
    val comics = comicService.loadComicFiles(path);

    val page = new StringBuilder()
    page.append("<html><body>")
    page.append("<p>")
    for (comic <- comics) {
      page.append("<span>")
      //page.append(comic.title)
      //val coverEncoded = new String(Base64.getEncoder().encode(comic.cover.data))
      //page.append("<img style=\"max-width: 100px;\" title=\"" + comic.title + "\" src=\"data:" + comic.cover.mediaType + ";base64,")
      //page.append(coverEncoded)
      //page.append("\">")
      page.append("<a href=\"comic?path=").append(URLEncoder.encode(comic.path))
        .append("&page=0\">")
      page.append(getImageTag(comic.title, comic.cover.mediaType, comic.cover.data, Some(150)))
      page.append("</a>")
      page.append("</span>")
    }
    page.append("</p>")
    page.append("</body></html>")

    page.toString()
  }

  @RequestMapping(Array("/comic"))
  def getComic(@RequestParam("path") path: String, @RequestParam("page") page: Int) = {
    comicService.readPage(path, page) match {
      case Some(comicPage) =>
        val html = new StringBuilder()
        html.append("<html><body>")
        html.append("<p>")
        if (page > 0) html.append("<a href=\"comic?path=").append(URLEncoder.encode(path))
            .append("&page=").append(page-1).append("\">previous</a>")
        html.append("<a href=\"comic?path=").append(URLEncoder.encode(path))
          .append("&page=").append(page+1).append("\">next</a>")
        html.append("</p>")
        html.append("<p>")
        html.append(getImageTag(path + " page " + page, comicPage.mediaType, comicPage.data, None))
        html.append("</p>")
        html.append("</body></html>")
    }
  }
}
