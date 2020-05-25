package com.cacoveanu.reader.controller

import java.net.URLEncoder
import java.nio.file.{Files, Paths}
import java.util.Base64

import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.service.{ComicService, FullComic}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody, RestController}

import scala.beans.BeanProperty

@Controller
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

  @RequestMapping(Array("/gesture"))
  def getGesturesTest(): String = {
    val byteArray = Files.readAllBytes(Paths.get("src/main/resources/static/index.html"))
    new String(byteArray)
  }

  case class UiComic(
                      @BeanProperty id: Long,
                      @BeanProperty title: String,
                      @BeanProperty cover: String
                    )

  private def base64Image(mediaType: MediaType, image: Array[Byte]) =
    "data:" + mediaType + ";base64," + new String(Base64.getEncoder().encode(image))

  @RequestMapping(Array("/rescan"))
  @ResponseBody
  def rescan() = {
    comicService.forceUpdateLibrary()
    ""
  }

  @RequestMapping(Array("/collection"))
  def getComicCollection(model: Model): String = {
    val comics = comicService.getCollection()
    val uiComics = comics.map(comic => UiComic(comic.id, comic.title, base64Image(comic.mediaType, comic.cover)))
    model.addAttribute("comics", uiComics.asJava)
    "collection"
  }

  @RequestMapping(Array("/comic"))
  def getComic(@RequestParam("id") id: Int, model: Model): String = {
    comicService.loadFullComic(id) match {
      case Some(FullComic(comic, pages)) =>
        model.addAttribute("id", id)
        model.addAttribute("pages", pages.size)
        model.addAttribute("startPage", 0)
        "comic"
      case _ => ""
    }
  }

  @RequestMapping(Array("/imageData"))
  @ResponseBody
  def getImageData(@RequestParam("id") id: Int, @RequestParam("page") page: Int): String = {
    comicService.loadFullComic(id) match {
      case Some(FullComic(_, pages)) if pages.indices contains page =>
        val comicPage = pages(page)
        val builder = new StringBuilder()
        builder.append("data:").append(comicPage.mediaType)
          .append(";base64,").append(new String(Base64.getEncoder().encode(comicPage.data))).toString()
      case _ => ""
    }
  }

  //@RequestMapping(Array("/comic"))
  /*def getComic(@RequestParam("path") path: String, @RequestParam("page") page: Int) = {
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
        html.toString()
      case None => null
    }
  }*/
}
