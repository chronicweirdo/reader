package com.cacoveanu.reader.controller

import java.net.URLDecoder
import java.security.Principal
import java.util.Date

import com.cacoveanu.reader.entity.Content
import com.cacoveanu.reader.service.{BookService, ContentService, EbookService, UserService}
import com.cacoveanu.reader.util.FileMediaTypes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView

@Controller
class EbookController @Autowired() (
                                     private val ebookService: EbookService,
                                     private val contentService: ContentService,
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
      case Some((book, resource, position)) => new RedirectView(s"/book?id=$bookId&path=$resource#$position" )
      case None => ResponseEntity.notFound().build()
    }
  }

  @RequestMapping(
    value=Array("/reportPosition"),
    method=Array(RequestMethod.PUT)
  )
  def reportPosition(@RequestParam("id") bookId: String, @RequestParam("link") link: String, @RequestParam("position") position: Int, principal: Principal) = {
    /*(userService.loadUser(principal.getName), comicService.loadComic(id)) match {
      case (Some(user), Some(comic)) if page < comic.totalPages =>
        comicService.saveComicProgress(new ComicProgress(user, comic, page, comic.totalPages, new Date()))
        new ResponseEntity[String](HttpStatus.OK)
      case _ => new ResponseEntity[String](HttpStatus.NOT_FOUND)
    }*/
    val decodedLink = URLDecoder.decode(link, "UTF-8")
    println(s"registering position for book $bookId resource $decodedLink position $position")

  }

}
