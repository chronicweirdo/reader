package com.cacoveanu.reader.controller

import java.security.Principal

import com.cacoveanu.reader.entity.Content
import com.cacoveanu.reader.service.{BookService, ContentService, UserService}
import com.cacoveanu.reader.util.{FileMediaTypes, FileTypes}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView

@Controller
class EbookController @Autowired() (private val contentService: ContentService,
                                     private val bookService: BookService,
                                     private val accountService: UserService) {

  @RequestMapping(Array("/book"))
  @ResponseBody
  def getBookResource(@RequestParam("id") bookId: String, @RequestParam("path") path: String) = {
    contentService.loadResource(bookId, path) match {
      case Some(Content(_, FileMediaTypes.TEXT_HTML_VALUE, bytes)) =>
        ResponseEntity.ok().body(new String(bytes, "UTF-8"))

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
  }

  @RequestMapping(Array("/openBook"))
  @ResponseBody
  def loadBook(@RequestParam("id") bookId: String, principal: Principal) = {
    accountService.loadUser(principal.getName)
      .flatMap(account => bookService.loadBookWithProgress(account, bookId)) match {
      case Some((book, resource, position)) => bookService.bookType(book) match {
        case FileTypes.CBR => new RedirectView(s"/comic?id=$bookId")
        case FileTypes.CBZ => new RedirectView(s"/comic?id=$bookId")
        case FileTypes.EPUB => new RedirectView(s"/book?id=$bookId&path=$resource#$position" )
      }
      case None => new RedirectView("/")
    }
  }

}
