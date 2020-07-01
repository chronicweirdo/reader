package com.cacoveanu.reader.controller

import com.cacoveanu.reader.service.EbookService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}
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

}
