package com.cacoveanu.reader.controller

import java.security.Principal
import java.{lang, util}
import java.util.{Base64, Date}

import com.cacoveanu.reader.entity.{ComicProgress, DbComic}

import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.service.{ComicService, UserService}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestBody, RequestMapping, RequestMethod, RequestParam, ResponseBody, RestController}

import scala.beans.BeanProperty

case class UiCollection(
                       @BeanProperty name: String,
                       @BeanProperty comics: util.List[UiComic]
                       )

case class UiComic(
                    @BeanProperty id: Long,
                    @BeanProperty collection: String,
                    @BeanProperty title: String,
                    @BeanProperty cover: String,
                    @BeanProperty progress: Int,
                    @BeanProperty pages: Int
                  )

class RemoveProgressForm {
  @BeanProperty var ids: java.util.List[Long] = _
}

case class CollectionPage(
                         @BeanProperty collections: java.util.List[String],
                         @BeanProperty comics: java.util.List[UiComic]
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

  @RequestMapping(Array("/collections"))
  def loadCollections(model: Model): String = {
    val collections = comicService.loadCollections().filter(c => ! c.isEmpty).asJava
    model.addAttribute("collections", collections)
    "collectionList"
  }

  @RequestMapping(value = Array("/search"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @ResponseBody
  def getComicsForPage(@RequestParam("term") term: String, @RequestParam("page") page: Int, principal: Principal, model: Model) = {
    val user = userService.loadUser(principal.getName)
    val comics: Seq[DbComic] = comicService.searchComics(term, page)
    val progress: Seq[ComicProgress] = comicService.loadComicProgress(user.get, comics)
    val progressByComic: Map[lang.Long, ComicProgress] = progress.map(p => (p.comic.id, p)).toMap

    val collections = comics.map(c => c.collection).toSet.toSeq.sorted
    val uiComics = comics
      .map(comic => UiComic(
        comic.id,
        comic.collection,
        comic.title,
        base64Image(comic.mediaType, comic.cover),
        progressByComic.get(comic.id).map(p => p.page).getOrElse(-1),
        progressByComic.get(comic.id).map(p => p.totalPages).getOrElse(-1)
      ))
    CollectionPage(collections.asJava, uiComics.asJava)
  }

  @RequestMapping(Array("/"))
  def getComicCollection(principal: Principal, model: Model): String = {
    val user = userService.loadUser(principal.getName)
    val progress: Seq[ComicProgress] = comicService.loadTopComicProgress(user.get, 6)

    val latestRead = progress
      .map(p => UiComic(
        p.comic.id,
        p.comic.collection,
        p.comic.title,
        base64Image(p.comic.mediaType, p.comic.cover),
        p.page,
        p.totalPages
      )).asJava

    model.addAttribute("user", principal.getName)
    model.addAttribute("latestRead", latestRead)
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



  @RequestMapping(
    value=Array("/removeProgress"),
    method=Array(RequestMethod.POST),
    consumes=Array(MediaType.APPLICATION_JSON_VALUE)
  )
  @ResponseBody
  def removeProgress(@RequestBody data: RemoveProgressForm, principal: Principal): String = {
    userService.loadUser(principal.getName) match {
      case Some(user) =>
        val progress: Seq[ComicProgress] = comicService.loadComicProgress(user)
        val progressToDelete = progress.filter(p => data.ids.asScala.contains(p.comic.id))
        comicService.deleteComicProgress(progressToDelete)
    }
    ""
  }

  @RequestMapping(Array("/imageData"))
  @ResponseBody
  def getImageData(@RequestParam("id") id: Int, @RequestParam("page") page: Int, principal: Principal): String = {
    (userService.loadUser(principal.getName), comicService.loadFullComic(id)) match {
      case (Some(user), Some(comic)) if comic.pages.indices contains page =>
        comicService.saveComicProgress(new ComicProgress(user, comic, page, comic.pages.size, new Date()))
        base64Image(comic.pages(page).mediaType, comic.pages(page).data)
      case _ => ""
    }
  }
}
