package com.cacoveanu.reader.controller

import java.nio.charset.StandardCharsets

import com.cacoveanu.reader.entity.Content
import com.cacoveanu.reader.service.{BookService, ContentService}
import com.cacoveanu.reader.util.{FileMediaTypes, FileUtil, WebUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}

import scala.beans.BeanProperty

case class ImageDataResponse(@BeanProperty image: String, @BeanProperty color: Array[Int])

@Controller
class ComicController @Autowired() (private val contentService: ContentService,
                                    private val bookService: BookService) {

  @RequestMapping(Array("/comic"))
  def loadComic(@RequestParam(name="id") id: String, model: Model): String = {
    bookService.loadBook(id) match {
      case Some(comic) =>
        model.addAttribute("id", id)
        model.addAttribute("pages", comic.size)
        model.addAttribute("title", comic.title)
        model.addAttribute("collection", comic.collection)
        model.addAttribute("cover", WebUtil.toBase64Image(comic.mediaType, comic.cover))
        "comic"
      case None => "error"
    }
  }

  private def getImageDataResponse(content: Content): ImageDataResponse = {
    val color = content.meta.getOrElse("color", Array(255, 255, 255)).asInstanceOf[Array[Int]]
    ImageDataResponse(WebUtil.toBase64Image(content.mediaType, content.data), color)
  }

  @RequestMapping(Array("/imageData"))
  def loadImageData(@RequestParam("id") id: String, @RequestParam("page") page: Int)/*: String*/ = {
    contentService
      .loadComicResources(id, contentService.getBatchForPosition(page))
      .find(p => p.index.isDefined && p.index.get == page)
      .map(c => ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(getImageDataResponse(c)))
      .getOrElse(WebUtil.notFound)
  }

  @RequestMapping(Array("/downloadPage"))
  @ResponseBody
  def download(@RequestParam("id") id: String, @RequestParam("page") page: Int) = {
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

  private def getFileName(id: String, page: Int, mediaType: String) =
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
