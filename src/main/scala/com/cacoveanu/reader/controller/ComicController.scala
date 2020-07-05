package com.cacoveanu.reader.controller

import java.security.Principal
import java.{lang, util}
import java.util.{Base64, Date}

import com.cacoveanu.reader.entity.{Book, Progress}

import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.service.{BookService, ComicService, ContentService, UserService}
import com.cacoveanu.reader.util.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView

import scala.beans.BeanProperty

/*case class UiCollection(
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
                  )*/





@Controller
class ComicController @Autowired() (private val comicService: ComicService,
                                    private val userService: UserService,
                                    private val contentService: ContentService,
                                    private val bookService: BookService) {

  /*private def base64Image(mediaType: String, image: Array[Byte]) =
    "data:" + mediaType + ";base64," + new String(Base64.getEncoder().encode(image))*/

  /*@RequestMapping(Array("/rescan"))
  @ResponseBody
  def rescan(@RequestParam(name="force", required = false) force: Boolean = false): RedirectView = {
    comicService.scan(force)
    new RedirectView("/")
  }*/











  @RequestMapping(Array("/comic"))
  def getComic(@RequestParam(name="id") id: String, model: Model, principal: Principal): String = {
    (userService.loadUser(principal.getName), comicService.loadComic(id)) match {
      case (Some(user), Some(comic)) =>
        val progress = bookService.loadProgress(user, comic)
        model.addAttribute("id", id)
        model.addAttribute("pages", comic.size)
        model.addAttribute("title", comic.title)
        model.addAttribute("collection", comic.collection)
        model.addAttribute("startPage", progress.map(p => p.position).getOrElse(0))
        "comic"
      case _ => ""
    }
  }

  /*@RequestMapping(
    value=Array("/removeProgress"),
    method=Array(RequestMethod.DELETE)
  )
  def removeProgressFromComic(@RequestParam("id") id: String, principal: Principal): ResponseEntity[String] = {
    (userService.loadUser(principal.getName), bookService.loadBook(id)) match {
      case (Some(user), Some(book)) =>
        bookService.loadProgress(user, book).foreach(bookService.deleteProgress)
        new ResponseEntity[String](HttpStatus.OK)
      case _ => new ResponseEntity[String](HttpStatus.NOT_FOUND)
    }
  }*/

  @RequestMapping(Array("/imageData"))
  @ResponseBody
  def getImageData(@RequestParam("id") id: String, @RequestParam("page") page: Int, principal: Principal): String = {
    val part = contentService.computePartNumberForPage(page)
    contentService.loadComicPart(id, part).find(p => p.index.isDefined && p.index.get == page) match {
      case Some(p) => WebUtil.toBase64Image(p.mediaType, p.data)
      case None => ""
    }
  }

  /*@RequestMapping(
    value=Array("/markProgress"),
    method=Array(RequestMethod.PUT)
  )
  def markProgress(@RequestParam("id") id: String, @RequestParam("page") page: Int, principal: Principal): ResponseEntity[String] = {
    (userService.loadUser(principal.getName), comicService.loadComic(id)) match {
      case (Some(user), Some(comic)) if page < comic.size =>
        val section = ""
        val finished = page == comic.size - 1
        bookService.saveProgress(new Progress(user, comic, section, page, new Date(), finished))
        new ResponseEntity[String](HttpStatus.OK)
      case _ => new ResponseEntity[String](HttpStatus.NOT_FOUND)
    }
  }*/
}
