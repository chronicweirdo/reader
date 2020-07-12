package com.cacoveanu.reader.controller

import com.cacoveanu.reader.entity.{Content, Setting}
import com.cacoveanu.reader.service.{BookService, ContentService, SettingService}
import com.cacoveanu.reader.util.{FileMediaTypes, FileTypes, FileUtil, HtmlUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView
import com.cacoveanu.reader.util.HtmlUtil.AugmentedHtmlString
import com.cacoveanu.reader.util.HtmlUtil.AugmentedJsoupDocument

@Controller
class BookController @Autowired()(private val contentService: ContentService,
                                  private val bookService: BookService,
                                  private val settingService: SettingService) {

  private def appendSettings(html: String): String = {
    val settings = Map("bookZoom" -> settingService.getSetting(Setting.BOOK_ZOOM))
    html.asHtml.addMeta(settings).asString
    //HtmlUtil.addMeta(html, settings)
  }

  private def toResponseEntity(content: Option[Content]) = content match {
    case Some(Content(_, FileMediaTypes.TEXT_HTML_VALUE, bytes)) =>
      ResponseEntity.ok().body(appendSettings(new String(bytes, "UTF-8")))

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

  @RequestMapping(Array("/book"))
  @ResponseBody
  def getBookResource(@RequestParam("id") bookId: String,
                      @RequestParam("path") path: String,
                      @RequestParam(name = "position", required = false) position: java.lang.Long) = {
    toResponseEntity(contentService.loadResource(bookId, path))
  }

  @RequestMapping(Array("/openBook"))
  @ResponseBody
  def loadBook(@RequestParam("id") bookId: String) = {
    bookService.loadBook(bookId) match {
      case Some(book) => FileUtil.getExtension(book.path) match {
        case FileTypes.CBR =>
          new RedirectView(s"/comic?id=$bookId")

        case FileTypes.CBZ =>
          new RedirectView(s"/comic?id=$bookId")

        case FileTypes.EPUB => bookService.loadProgress(book) match {
          case Some(progress) => new RedirectView(s"/book?id=$bookId&path=${progress.section}&position=${progress.position}")
          case None => new RedirectView(s"/book?id=$bookId&path=${book.getSections()(0).link}")
        }
      }

      case _ => new RedirectView("/") // todo: maybe better to throw a not found
    }
  }

}
