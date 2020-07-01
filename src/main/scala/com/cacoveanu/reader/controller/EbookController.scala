package com.cacoveanu.reader.controller

import java.net.URLDecoder
import java.security.Principal
import java.util.Date

import com.cacoveanu.reader.entity.ComicProgress
import com.cacoveanu.reader.service.EbookService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView

@Controller
class EbookController @Autowired() (private val ebookService: EbookService) {

  @RequestMapping(Array("/book"))
  @ResponseBody
  def getBookResource(@RequestParam("id") bookId: String, @RequestParam("path") path: String) = {
    ebookService.loadResource(bookId, path) match {
      case Some((contentType, bytes)) =>
        contentType match {
          case "text/html" => ResponseEntity.ok().body(new String(bytes, "UTF-8"))
          case "text/css" => ResponseEntity.ok().body(new String(bytes, "UTF-8"))
          case "image/jpeg" => ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes)
          case _ => ResponseEntity.notFound().build()
        }
      case None => ResponseEntity.notFound().build()
    }
  }

  @RequestMapping(Array("/loadBook"))
  @ResponseBody
  def loadBook(@RequestParam("id") bookId: String) = {
    // load book progress
    val resource = "text/part0004.html"
    val position = 2000
    // redirect to book resource based on progess
    new RedirectView(s"/book?id=$bookId&path=$resource#$position" )
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
