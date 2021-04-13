package com.cacoveanu.reader.controller

import com.cacoveanu.reader.entity.{BookTocEntry, Content, TocNode}
import com.cacoveanu.reader.service.{BookService, ContentService}
import com.cacoveanu.reader.util.{BookNode, FileTypes, FileUtil, WebUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpHeaders, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView
import org.springframework.ui.Model

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@Controller
class BookController @Autowired()(private val contentService: ContentService,
                                  private val bookService: BookService) {

  @RequestMapping(Array("/book"))
  def loadBook(@RequestParam(name="id") id: java.lang.Long, model: Model): String = {
    bookService.loadBook(id) match {
      case Some(book) =>
        model.addAttribute("id", id)
        model.addAttribute("size", book.size)
        model.addAttribute("title", book.title)
        model.addAttribute("collection", book.collection)
        model.addAttribute("bookStart", 0)
        model.addAttribute("bookEnd", book.size - 1)
        val tocTree = TocNode.getTocTree(book.toc.asScala.toSeq)
        model.addAttribute("tableOfContents", tocTree)
        "book"
      case None => "error"
    }
  }

  @RequestMapping(Array("/bookSection"))
  @ResponseBody
  def loadBookSection(@RequestParam("id") id: java.lang.Long, @RequestParam("position") position: java.lang.Long): ResponseEntity[BookNode] = {
    val sectionStartPosition = contentService.findStartPositionForSectionContaining(id, position)
    val node = contentService.loadBookSection(id, sectionStartPosition)
    val headers = new HttpHeaders()
    headers.set("sectionStart", node.start.toString)
    headers.set("sectionEnd", node.end.toString)
    ResponseEntity.ok().headers(headers).body(node)
  }

  @RequestMapping(Array("/bookResource"))
  def loadBookResource(@RequestParam("id") id: java.lang.Long, @RequestParam("path") path: String) = {
    contentService.loadBookResource(id, path)
      .map(content => WebUtil.toResponseEntity(content.mediaType, content.data))
      .getOrElse(WebUtil.notFound)
  }
}

case class UiToc(@BeanProperty index: Int, @BeanProperty title: String, @BeanProperty position: Long)