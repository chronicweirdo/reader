package com.cacoveanu.reader.controller

import com.cacoveanu.reader.entity.{Book, Progress}
import com.cacoveanu.reader.service.{BookService, ScannerService, UserService}
import com.cacoveanu.reader.util.{SessionUtil, WebUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@Controller
class MainController @Autowired()(
                                   private val bookService: BookService,
                                   //private val settingService: SettingService,
                                   private val scannerService: ScannerService) {

  @RequestMapping(Array("/collections"))
  def loadCollections(model: Model): String = {
    val collections = bookService.loadCollections().filter(c => ! c.isEmpty).asJava
    model.addAttribute("collections", collections)
    "collectionList"
  }

  @RequestMapping(Array("/help"))
  def loadHelp(): String = "help"

  @RequestMapping(Array("/more"))
  def morePage(model: Model): String = {
    model.addAttribute("admin", SessionUtil.getUser().admin)
    "more"
  }

  @RequestMapping(value = Array("/search"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @ResponseBody
  def search(@RequestParam("term") term: String, @RequestParam("page") page: Int): ResponseEntity[CollectionPage] = {
    val books: Seq[Book] = if (term.isEmpty) bookService.getCollectionPage(page)
      else bookService.search(term, page)

    val progress: Seq[Progress] = bookService.loadProgress(books)
    val progressByBook: Map[java.lang.Long, Progress] = progress.map(p => (p.book.id, p)).toMap

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
    new ResponseEntity[CollectionPage](CollectionPage(collections.asJava, uiBooks.asJava), HttpStatus.OK)
  }

  @RequestMapping(Array("/"))
  def loadMainPage(model: Model): String = {
    val progress: Seq[Progress] = bookService.loadTopProgress(6)

    val latestRead = progress
      .map(p => UiBook(
        p.book.id,
        p.book.collection,
        p.book.title,
        WebUtil.toBase64Image(p.book.mediaType, p.book.cover),
        p.position,
        p.book.size
      )).asJava

    model.addAttribute("user", SessionUtil.getUser().username)
    model.addAttribute("latestRead", latestRead)
    "library"
  }

  @RequestMapping(
    value=Array("/removeProgress"),
    method=Array(RequestMethod.DELETE)
  )
  def removeProgress(@RequestParam("id") id: java.lang.Long): ResponseEntity[String] = {
    if (bookService.deleteProgress(id)) new ResponseEntity[String](HttpStatus.OK)
    else new ResponseEntity[String](HttpStatus.NOT_FOUND)
  }

  @RequestMapping(
    value=Array("/markProgress"),
    method=Array(RequestMethod.PUT)
  )
  def markProgress(@RequestParam("id") id: java.lang.Long,
                   @RequestParam("position") position: Int): ResponseEntity[String] = {
    if (bookService.saveProgress(id, position)) new ResponseEntity[String](HttpStatus.OK)
    else new ResponseEntity[String](HttpStatus.NOT_FOUND)
  }

  /*@RequestMapping(
    value=Array("/updateSetting"),
    method=Array(RequestMethod.PUT)
  )
  def updateSetting(@RequestParam("name") name: String, @RequestParam("value") value: String) = {
    settingService.saveSetting(name, value)
  }*/

  /*@RequestMapping(
    value=Array("/loadSettings"),
    method=Array(RequestMethod.GET)
  )
  @ResponseBody
  def loadSettings(@RequestParam("name") name: Seq[String]) = {
    name.map(n => (n, settingService.getSetting(n))).toMap.asJava
  }*/

  @RequestMapping(
    value=Array("/scan"),
    method=Array(RequestMethod.GET)
  )
  def scan() = {
    scannerService.scan()
    new RedirectView("/")
  }
}

case class CollectionPage(
                           @BeanProperty collections: java.util.List[String],
                           @BeanProperty books: java.util.List[UiBook]
                         )

case class UiBook(
                    @BeanProperty id: java.lang.Long,
                    @BeanProperty collection: String,
                    @BeanProperty title: String,
                    @BeanProperty cover: String,
                    @BeanProperty progress: Int,
                    @BeanProperty pages: Int
                  )
