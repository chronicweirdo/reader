package com.cacoveanu.reader.controller

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util

import com.cacoveanu.reader.entity.Content
import com.cacoveanu.reader.service.{BookService, ContentService}
import com.cacoveanu.reader.util.{FileMediaTypes, FileTypes, FileUtil, WebUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView
import org.springframework.ui.Model

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import scala.util.Random

@Controller
class BookController @Autowired()(private val contentService: ContentService,
                                  private val bookService: BookService) {

  @RequestMapping(Array("/book"))
  def loadBook(@RequestParam(name="id") id: java.lang.Long, model: Model): String = {
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
      case None => "error"
    }
  }

  @RequestMapping(Array("/bookSection"))
  @ResponseBody
  def loadBookSection(@RequestParam("id") id: java.lang.Long, @RequestParam("position") position: java.lang.Long) = {
    val sectionStartPosition = contentService.findStartPositionForSectionContaining(id, position)
    val node = contentService.loadBookSection(id, sectionStartPosition)
    node
  }

  @RequestMapping(Array("/bookResource"))
  def loadBookResource(@RequestParam("id") id: java.lang.Long, @RequestParam("path") path: String) = {
    contentService.loadBookResource(id, path)
      .map(content => WebUtil.toResponseEntity(content.mediaType, content.data))
      .getOrElse(WebUtil.notFound)
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

      case _ => new RedirectView("/error")
    }
  }

}

case class UiToc(@BeanProperty index: Int, @BeanProperty title: String, @BeanProperty position: Long)