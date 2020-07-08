package com.cacoveanu.reader.controller

import com.cacoveanu.reader.service.{BookService, ContentService, UserService}
import com.cacoveanu.reader.util.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}

@Controller
class ComicController @Autowired() (private val userService: UserService,
                                    private val contentService: ContentService,
                                    private val bookService: BookService) {

  @RequestMapping(Array("/comic"))
  def getComic(@RequestParam(name="id") id: String, model: Model): String = {
    bookService.loadBook(id) match {
      case Some(comic) =>
        val progress = bookService.loadProgress(comic)
        model.addAttribute("id", id)
        model.addAttribute("pages", comic.size)
        model.addAttribute("title", comic.title)
        model.addAttribute("collection", comic.collection)
        model.addAttribute("startPage", progress.map(p => p.position).getOrElse(0))
        "comic"
      case None => "" // todo: throw some error!
    }
  }

  @RequestMapping(Array("/imageData"))
  @ResponseBody
  def getImageData(@RequestParam("id") id: String, @RequestParam("page") page: Int): String = {
    // todo: replace this to getting a list of positions for batch, then load them using an extended load book resource method (for multiple indexes)
    val part = contentService.computePartNumberForPage(page)
    contentService.loadComicPart(id, part).find(p => p.index.isDefined && p.index.get == page) match {
      case Some(p) => WebUtil.toBase64Image(p.mediaType, p.data)
      case None => ""
    }
  }

}
