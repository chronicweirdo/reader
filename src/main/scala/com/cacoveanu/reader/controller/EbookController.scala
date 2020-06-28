package com.cacoveanu.reader.controller

import com.cacoveanu.reader.service.EbookService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}

@Controller
class EbookController @Autowired() (private val ebookService: EbookService) {

  @RequestMapping(Array("/book"))
  @ResponseBody
  def getBookResource(@RequestParam("id") bookId: String, @RequestParam("path") path: String) = {
    ebookService.loadResource(bookId, path).getOrElse("")
  }

}
