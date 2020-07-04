package com.cacoveanu.reader.controller

import java.security.Principal
import java.{lang, util}
import java.util.{Base64, Date}

import com.cacoveanu.reader.entity.{BookProgress, DbBook}

import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.service.{ComicService, UserService}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView

import scala.beans.BeanProperty

case class UiCollection(
                       @BeanProperty name: String,
                       @BeanProperty comics: util.List[UiComic]
                       )

case class UiComic(
                    @BeanProperty id: String,
                    @BeanProperty collection: String,
                    @BeanProperty title: String,
                    @BeanProperty cover: String,
                    @BeanProperty progress: Int,
                    @BeanProperty pages: Int
                  )

class ChangePasswordForm {
  @BeanProperty var oldPassword: String = _
  @BeanProperty var newPassword: String = _
  @BeanProperty var newPasswordConfirm: String = _
}

case class CollectionPage(
                         @BeanProperty collections: java.util.List[String],
                         @BeanProperty comics: java.util.List[UiComic]
                         )

@Controller
class ComicController @Autowired() (private val comicService: ComicService, private val userService: UserService) {

  private def base64Image(mediaType: String, image: Array[Byte]) =
    "data:" + mediaType + ";base64," + new String(Base64.getEncoder().encode(image))

  /*@RequestMapping(Array("/rescan"))
  @ResponseBody
  def rescan(@RequestParam(name="force", required = false) force: Boolean = false): RedirectView = {
    comicService.scan(force)
    new RedirectView("/")
  }*/

  @RequestMapping(Array("/collections"))
  def loadCollections(model: Model): String = {
    val collections = comicService.loadCollections().filter(c => ! c.isEmpty).asJava
    model.addAttribute("collections", collections)
    "collectionList"
  }

  @RequestMapping(Array("/help"))
  def loadHelp(): String = "help"

  @RequestMapping(Array("/settings"))
  def loadSettings(): String = "settings"

  @RequestMapping(value=Array("/password"), method = Array(RequestMethod.GET))
  def passwordResetPage(): String = "passwordReset"

  @RequestMapping(
    value=Array("/password"),
    method=Array(RequestMethod.POST),
    consumes=Array(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  )
  @ResponseBody
  def passwordResetAction(body: ChangePasswordForm, principal: Principal): RedirectView = {
    if (body.newPassword != body.newPasswordConfirm) {
      new RedirectView("/password?error")
    } else {
      userService.loadUser(principal.getName) match {
        case Some(user) =>
          if (userService.changePassword(user, body.oldPassword, body.newPassword)) {
            new RedirectView("/")
          } else {
            new RedirectView("/password?error")
          }
        case None => new RedirectView("/password?error")
      }
    }

  }

  @RequestMapping(value = Array("/search"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @ResponseBody
  def getComicsForPage(@RequestParam("term") term: String, @RequestParam("page") page: Int, principal: Principal, model: Model) = {
    val user = userService.loadUser(principal.getName)
    val comics: Seq[DbBook] = if (term.isEmpty) comicService.getCollectionPage(page)
      else comicService.searchComics(term, page)
    val progress: Seq[BookProgress] = comicService.loadComicProgress(user.get, comics)
    val progressByComic: Map[String, BookProgress] = progress.map(p => (p.book.id, p)).toMap

    val collections = comics.map(c => c.collection).toSet.toSeq.sorted
    val uiComics = comics
      .map(comic => UiComic(
        comic.id,
        comic.collection,
        comic.title,
        base64Image(comic.mediaType, comic.cover),
        progressByComic.get(comic.id).map(p => p.position).getOrElse(-1),
        progressByComic.get(comic.id).map(p => p.book.size).getOrElse(-1)
      ))
    CollectionPage(collections.asJava, uiComics.asJava)
  }

  @RequestMapping(Array("/"))
  def getComicCollection(principal: Principal, model: Model): String = {
    val user = userService.loadUser(principal.getName)
    val progress: Seq[BookProgress] = comicService.loadTopComicProgress(user.get, 6)

    val latestRead = progress
      .map(p => UiComic(
        p.book.id,
        p.book.collection,
        p.book.title,
        base64Image(p.book.mediaType, p.book.cover),
        p.position,
        p.book.size
      )).asJava

    model.addAttribute("user", principal.getName)
    model.addAttribute("latestRead", latestRead)
    "collection"
  }

  @RequestMapping(Array("/comic"))
  def getComic(@RequestParam(name="id") id: String, model: Model, principal: Principal): String = {
    (userService.loadUser(principal.getName), comicService.loadComic(id)) match {
      case (Some(user), Some(comic)) =>
        val progress = comicService.loadComicProgress(user, comic)
        model.addAttribute("id", id)
        model.addAttribute("pages", comic.size)
        model.addAttribute("title", comic.title)
        model.addAttribute("collection", comic.collection)
        model.addAttribute("startPage", progress.map(p => p.position).getOrElse(0))
        "comic"
      case _ => ""
    }
  }

  @RequestMapping(
    value=Array("/removeProgress"),
    method=Array(RequestMethod.DELETE)
  )
  def removeProgressFromComic(@RequestParam("id") id: String, principal: Principal): ResponseEntity[String] = {
    userService.loadUser(principal.getName) match {
      case Some(user) =>
        comicService.loadComicProgress(user, id) match {
          case Some(progress) =>
            comicService.deleteComicProgress(progress)
            new ResponseEntity[String](HttpStatus.OK)
          case None =>
            new ResponseEntity[String](HttpStatus.NOT_FOUND)
        }
      case None => new ResponseEntity[String](HttpStatus.UNAUTHORIZED)
    }
  }

  @RequestMapping(Array("/imageData"))
  @ResponseBody
  def getImageData(@RequestParam("id") id: String, @RequestParam("page") page: Int, principal: Principal): String = {
    (userService.loadUser(principal.getName), comicService.loadComicPart(id, comicService.computePartNumberForPage(page))) match {
      case (Some(user), comicPages) =>
        comicPages.find(p => p.num == page) match {
          case Some(p) => base64Image(p.mediaType, p.data)
          case None => ""
        }
      case _ => ""
    }
  }

  @RequestMapping(
    value=Array("/markProgress"),
    method=Array(RequestMethod.PUT)
  )
  def markProgress(@RequestParam("id") id: String, @RequestParam("page") page: Int, principal: Principal): ResponseEntity[String] = {
    (userService.loadUser(principal.getName), comicService.loadComic(id)) match {
      case (Some(user), Some(comic)) if page < comic.size =>
        // user: DbUser, book: DbBook, section: String, position: Int, lastUpdate: Date, finished: Boolean
        val section = ""
        val finished = page == comic.size - 1
        comicService.saveComicProgress(new BookProgress(user, comic, section, page, new Date(), finished))
        new ResponseEntity[String](HttpStatus.OK)
      case _ => new ResponseEntity[String](HttpStatus.NOT_FOUND)
    }
  }
}
