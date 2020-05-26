package com.cacoveanu.reader.controller

import java.net.URLEncoder
import java.nio.file.{Files, Paths}
import java.security.Principal
import java.util
import java.util.Base64

import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.service.{ComicProgress, ComicService, UserService}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody, RestController}

import scala.beans.BeanProperty

case class UiCollection(
                       @BeanProperty name: String,
                       @BeanProperty comics: util.List[UiComic]
                       )

case class UiComic(
                    @BeanProperty id: Long,
                    @BeanProperty title: String,
                    @BeanProperty cover: String
                  )

@Controller
class ComicController @Autowired() (private val comicService: ComicService, private val userService: UserService) {

  private def base64Image(mediaType: MediaType, image: Array[Byte]) =
    "data:" + mediaType + ";base64," + new String(Base64.getEncoder().encode(image))

  @RequestMapping(Array("/rescan"))
  @ResponseBody
  def rescan() = {
    comicService.forceUpdateLibrary()
    ""
  }

  @RequestMapping(Array("/"))
  def getComicCollection(principal: Principal, model: Model): String = {
    val comics = comicService.getCollection()
    val uiComics = comics
      .groupBy(comic => comic.collection)
      .map { case (collection, entries) => UiCollection(
        collection,
        entries.map(comic => UiComic(comic.id, comic.title, base64Image(comic.mediaType, comic.cover))).asJava
      )}
      .toSeq
      .sortBy(collection => collection.name)


    val comicsCollection: util.List[UiCollection] = uiComics.asJava
    model.addAttribute("user", principal.getName)
    model.addAttribute("comicCollections", comicsCollection)
    "collection"
  }

  @RequestMapping(Array("/comic"))
  def getComic(@RequestParam(name="id") id: Int, model: Model, principal: Principal): String = {
    (userService.loadUser(principal.getName), comicService.loadFullComic(id)) match {
      case (Some(user), Some(comic)) =>
        val progress = comicService.loadComicProgress(user, comic)
        model.addAttribute("id", id)
        model.addAttribute("pages", comic.pages.size)
        model.addAttribute("startPage", progress.map(p => p.page).getOrElse(0))
        "comic"
      case _ => ""
    }
  }

  @RequestMapping(Array("/imageData"))
  @ResponseBody
  def getImageData(@RequestParam("id") id: Int, @RequestParam("page") page: Int, principal: Principal): String = {
    (userService.loadUser(principal.getName), comicService.loadFullComic(id)) match {
      case (Some(user), Some(comic)) if comic.pages.indices contains page =>
        val p = new ComicProgress()
        p.user = user
        p.comic = comic
        p.page = page
        comicService.saveComicProgress(p)
        base64Image(comic.pages(page).mediaType, comic.pages(page).data)
      case _ => ""
    }
  }
}
