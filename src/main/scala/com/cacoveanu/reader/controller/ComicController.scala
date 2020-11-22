package com.cacoveanu.reader.controller

import java.nio.charset.StandardCharsets

import com.cacoveanu.reader.service.{BookService, ContentService, UserService}
import com.cacoveanu.reader.util.{FileMediaTypes, FileUtil, WebUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}

@Controller
class ComicController @Autowired() (private val userService: UserService,
                                    private val contentService: ContentService,
                                    private val bookService: BookService) {

  @RequestMapping(Array("/comic"))
  def loadComic(@RequestParam(name="id") id: java.lang.Long, model: Model): String = {
    bookService.loadBook(id) match {
      case Some(comic) =>
        val progress = bookService.loadProgress(comic)
        model.addAttribute("id", id)
        model.addAttribute("pages", comic.size)
        model.addAttribute("title", comic.title)
        model.addAttribute("collection", comic.collection)
        model.addAttribute("startPage", progress.map(p => p.position).getOrElse(0))
        "comic"
      case None => "error"
    }
  }

  @RequestMapping(Array("/imageData"))
  def loadImageData(@RequestParam("id") id: java.lang.Long, @RequestParam("page") page: Int)/*: String*/ = {
    contentService
      .loadComicResources(id, contentService.getBatchForPosition(page))
      .find(p => p.index.isDefined && p.index.get == page)
      .map(p => (MediaType.TEXT_PLAIN_VALUE, WebUtil.toBase64Image(p.mediaType, p.data)))
      .map(t => WebUtil.toResponseEntity(t._1, t._2.getBytes(StandardCharsets.UTF_8)))
      .getOrElse(WebUtil.notFound)
  }

  @RequestMapping(Array("/downloadPage"))
  @ResponseBody
  def download(@RequestParam("id") id: java.lang.Long, @RequestParam("page") page: Int) = {
    contentService
      .loadComicResources(id, contentService.getBatchForPosition(page))
      .find(p => p.index.isDefined && p.index.get == page)
      .map(p => ResponseEntity
        .ok()
        .contentType(toContentType(p.mediaType))
        .header("Content-Disposition", "attachment; filename=\"" + getFileName(id, page, p.mediaType) + "\"")
        .body(p.data)
      ).getOrElse(WebUtil.notFound)
  }

  private def getFileName(id: java.lang.Long, page: Int, mediaType: String) =
    FileUtil.getExtensionForMediaType(mediaType) match {
      case Some(ext) => s"$id-$page.${ext}"
      case None => s"$id-$page"
    }

  private def toContentType(mediaType: String) =
    mediaType match {
      case FileMediaTypes.IMAGE_PNG_VALUE => MediaType.IMAGE_PNG
      case FileMediaTypes.IMAGE_GIF_VALUE => MediaType.IMAGE_GIF
      case _ => MediaType.IMAGE_JPEG
    }

}
