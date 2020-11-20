package com.cacoveanu.reader.controller

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util

import com.cacoveanu.reader.entity.{Content, Setting}
import com.cacoveanu.reader.service.{BookService, ContentService, SettingService}
import com.cacoveanu.reader.util.{FileMediaTypes, FileTypes, FileUtil, HtmlUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView
import com.cacoveanu.reader.util.HtmlUtil.AugmentedHtmlString
import com.cacoveanu.reader.util.HtmlUtil.AugmentedJsoupDocument
import org.springframework.ui.Model

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import scala.util.Random

@Controller
class BookController @Autowired()(private val contentService: ContentService,
                                  private val bookService: BookService,
                                  private val settingService: SettingService) {

  private def appendSettings(html: String): String = {
    val settings = Map("bookZoom" -> settingService.getSetting(Setting.BOOK_ZOOM))
    html.asHtml.addMeta(settings).asString
    //HtmlUtil.addMeta(html, settings)
  }

  private def toResponseEntity(content: Option[Content]) = content match {
    case Some(Content(_, FileMediaTypes.TEXT_HTML_VALUE, bytes)) =>
      ResponseEntity.ok().body(appendSettings(new String(bytes, "UTF-8")))

    case Some(Content(_, FileMediaTypes.TEXT_CSS_VALUE, bytes)) =>
      ResponseEntity.ok().body(new String(bytes, "UTF-8"))

    case Some(Content(_, FileMediaTypes.IMAGE_JPEG_VALUE, bytes)) =>
      ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes)

    case Some(Content(_, FileMediaTypes.IMAGE_PNG_VALUE, bytes)) =>
      ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes)

    case Some(Content(_, FileMediaTypes.IMAGE_GIF_VALUE, bytes)) =>
      ResponseEntity.ok().contentType(MediaType.IMAGE_GIF).body(bytes)

    case _ => ResponseEntity.notFound().build()
  }

  @RequestMapping(Array("/positions_test"))
  def getPositionsTest() = "positions_test"

  val positionsTestBookId = 8891
  val positionsTestBookPath = "text%2Fpart0005.html"

  case class PositionsTestResponse(@BeanProperty content: String, @BeanProperty sizeWithoutSpaces: Int)

  @RequestMapping(Array("/positions_test_computed_data"))
  @ResponseBody
  def getPositionsTestComputedData() = {
    val content: Content = contentService.loadResource(positionsTestBookId, URLDecoder.decode(positionsTestBookPath, StandardCharsets.UTF_8.name())).get
    HtmlUtil.positionsTestGet(content.data)
  }

  @RequestMapping(Array("/book"))
  def getBook(@RequestParam(name="id") id: java.lang.Long, model: Model): String = {
    bookService.loadBook(id) match {
      case Some(book) =>
        val progress = bookService.loadProgress(book)
        model.addAttribute("id", id)
        model.addAttribute("size", book.size)
        model.addAttribute("title", book.title)
        model.addAttribute("collection", book.collection)
        model.addAttribute("startPosition", progress.map(p => p.position).getOrElse(0))
        model.addAttribute("bookStart", 0)
        model.addAttribute("bookEnd", book.size - 1)
        val uiToc: util.List[UiToc] = book.toc.asScala.map(e => UiToc(e.index, e.title, e.position)).sortBy(_.position).asJava
        model.addAttribute("tableOfContents", uiToc)
        "book"
      case None => "" // todo: throw some error!
    }
  }

  @RequestMapping(Array("/bookSection"))
  @ResponseBody
  def loadResource(@RequestParam("id") id: java.lang.Long, @RequestParam("position") position: java.lang.Long) = {
    val node = contentService.loadBookResource(id, position)
    node
  }

  @RequestMapping(Array("/bookResource"))
  def loadBookResource(@RequestParam("id") id: java.lang.Long, @RequestParam("path") path: String) = {
    toResponseEntity(contentService.loadResource(id, path))
  }

  @RequestMapping(Array("/openBook"))
  @ResponseBody
  def open(@RequestParam("id") bookId: java.lang.Long) = {
    bookService.loadBook(bookId) match {
      case Some(book) => FileUtil.getExtension(book.path) match {
        case FileTypes.CBR =>
          new RedirectView(s"/comic?id=$bookId")

        case FileTypes.CBZ =>
          new RedirectView(s"/comic?id=$bookId")

        case FileTypes.PDF =>
          new RedirectView(s"/comic?id=$bookId")

        case FileTypes.EPUB =>
          new RedirectView(s"/book?id=$bookId")
      }

      case _ => new RedirectView("/") // todo: maybe better to throw a not found
    }
  }

}

case class UiToc(@BeanProperty index: Int, @BeanProperty title: String, @BeanProperty position: Int)