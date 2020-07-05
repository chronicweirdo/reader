package com.cacoveanu.reader.controller

import java.security.Principal
import java.util
import java.util.Date

import com.cacoveanu.reader.entity.{Book, Progress}
import com.cacoveanu.reader.service.{BookService, UserService}
import com.cacoveanu.reader.util.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@Controller
class MainController @Autowired()(
                                   private val bookService: BookService,
                                   private val accountService: UserService) {

  @RequestMapping(Array("/collections"))
  def loadCollections(model: Model): String = {
    val collections = bookService.loadCollections().filter(c => ! c.isEmpty).asJava
    model.addAttribute("collections", collections)
    "collectionList"
  }

  @RequestMapping(Array("/help"))
  def loadHelp(): String = "help"

  @RequestMapping(Array("/settings"))
  def loadSettings(): String = "settings"

  @RequestMapping(value = Array("/search"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @ResponseBody
  def search(@RequestParam("term") term: String, @RequestParam("page") page: Int, principal: Principal, model: Model) = {
    val user = accountService.loadUser(principal.getName)
    val books: Seq[Book] = if (term.isEmpty) bookService.getCollectionPage(page)
    else bookService.search(term, page)
    val progress: Seq[Progress] = bookService.loadProgress(user.get, books) // todo: check if user really exists
    val progressByBook: Map[String, Progress] = progress.map(p => (p.book.id, p)).toMap

    val collections = books.map(c => c.collection).distinct.sorted
    val uiBooks = books
      .map(book => UiBook(
        book.id,
        book.collection,
        book.title,
        WebUtil.toBase64Image(book.mediaType, book.cover),
        progressByBook.get(book.id).map(p => p.position).getOrElse(-1),
        progressByBook.get(book.id).map(p => p.book.size).getOrElse(-1)
      ))
    CollectionPage(collections.asJava, uiBooks.asJava)
  }

  @RequestMapping(Array("/"))
  def loadMainPage(principal: Principal, model: Model): String = {
    val user = accountService.loadUser(principal.getName)
    val progress: Seq[Progress] = bookService.loadTopProgress(user.get, 6) // todo: check if user really exists

    val latestRead = progress
      .map(p => UiBook(
        p.book.id,
        p.book.collection,
        p.book.title,
        WebUtil.toBase64Image(p.book.mediaType, p.book.cover),
        p.position,
        p.book.size
      )).asJava

    model.addAttribute("user", principal.getName)
    model.addAttribute("latestRead", latestRead)
    "collection"
  }


  @RequestMapping(
    value=Array("/removeProgress"),
    method=Array(RequestMethod.DELETE)
  )
  def removeProgressFromComic(@RequestParam("id") id: String, principal: Principal): ResponseEntity[String] = {
    (accountService.loadUser(principal.getName), bookService.loadBook(id)) match {
      case (Some(user), Some(book)) =>
        bookService.loadProgress(user, book).foreach(bookService.deleteProgress)
        new ResponseEntity[String](HttpStatus.OK)
      case _ => new ResponseEntity[String](HttpStatus.NOT_FOUND)
    }
  }

  @RequestMapping(
    value=Array("/markProgress"),
    method=Array(RequestMethod.PUT)
  )
  def markProgress(
                    @RequestParam("id") id: String,
                    @RequestParam("section") section: String,
                    @RequestParam("position") position: Int,
                    principal: Principal): ResponseEntity[String] = {
    (accountService.loadUser(principal.getName), bookService.loadBook(id)) match {
      case (Some(user), Some(book)) if position < book.size =>
        val finished = position >= book.size - 1
        bookService.saveProgress(new Progress(user, book, section, position, new Date(), finished))
        new ResponseEntity[String](HttpStatus.OK)
      case _ => new ResponseEntity[String](HttpStatus.NOT_FOUND)
    }
  }
}

case class CollectionPage(
                           @BeanProperty collections: java.util.List[String],
                           @BeanProperty books: java.util.List[UiBook]
                         )

/*case class UiCollection(
                         @BeanProperty name: String,
                         @BeanProperty books: util.List[UiBook]
                       )*/

case class UiBook(
                    @BeanProperty id: String,
                    @BeanProperty collection: String,
                    @BeanProperty title: String,
                    @BeanProperty cover: String,
                    @BeanProperty progress: Int,
                    @BeanProperty pages: Int
                  )
