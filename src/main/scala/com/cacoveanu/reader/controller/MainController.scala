package com.cacoveanu.reader.controller

import com.cacoveanu.reader.entity.{Book, Progress}
import com.cacoveanu.reader.service.{BookService, ScannerService}
import com.cacoveanu.reader.util.{FileTypes, FileUtil, LogoUtil, SessionUtil, WebUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}

import java.util.Date
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@Controller
class MainController @Autowired()(
                                   private val bookService: BookService,
                                   private val scannerService: ScannerService) {

  @Autowired
  private var environment: Environment = _

  val CONST_LINE_REGEX = "const\\s*([^=\\s]+)\\s*=.+".r

  def unwrapStringValue(value: String): String =
    if (value.startsWith("\"") && value.endsWith("\"")) {
      value.substring(1, value.length - 1)
    } else if (value.startsWith("'") && value.endsWith("'")) {
      value.substring(1, value.length - 1)
    } else value

  def constructConstLine(constName: String, constValue: String): String = {
    s"const ${constName} = '${unwrapStringValue(constValue)}'"
  }

  def processJsLine(line: String): String = {
    val constLineMatcher = CONST_LINE_REGEX.pattern.matcher(line)
    if (constLineMatcher.matches()) {
      val settingName = constLineMatcher.group(1)
      val settingValue = environment.getProperty(settingName)
      if (settingValue != null) {
        constructConstLine(settingName, settingValue)
      } else {
        line
      }
    } else {
      line
    }
  }

  def processJsTemplate(path: String): String = {
    val source = scala.io.Source.fromInputStream(new ClassPathResource(path).getInputStream())
    val lines: Seq[String] = try source.getLines().toSeq finally source.close()
    lines.map(line => processJsLine(line)).mkString("\n")
  }


  @RequestMapping(value = Array("/settings.js"), produces = Array("application/javascript"))
  def loadSettingsJs(): ResponseEntity[String] = {
    val content = processJsTemplate("/static/settings_template.js")
    new ResponseEntity[String](content, HttpStatus.OK)
  }

  @RequestMapping(value = Array("/logo.png"), produces = Array(MediaType.IMAGE_PNG_VALUE))
  @ResponseBody
  def loadLogoPng(@RequestParam(name = "size", required = false, defaultValue = "64") size: Integer) = {
    val backgroundColor = if (environment.getProperty("LOGO_BACKGROUND") != null) {
      unwrapStringValue(environment.getProperty("LOGO_BACKGROUND"))
    } else "#ffd700"
    val foregroundColor = if (environment.getProperty("LOGO_FOREGROUND") != null) {
      unwrapStringValue(environment.getProperty("LOGO_FOREGROUND"))
    } else "#000000"
    val imageBytes = LogoUtil.generate("/static/logo.svg", backgroundColor, foregroundColor, size.toFloat)
    ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes)
  }

  @RequestMapping(Array("/collections"))
  def loadCollections(model: Model): String = {
    val tree = bookService.loadCollectionsTree()
    model.addAttribute("tree", tree)
    "collectionList"
  }

  @RequestMapping(Array("/help"))
  def loadHelp(): String = "help"

  @RequestMapping(Array("/history"))
  def loadHistory(): String = "history"

  @RequestMapping(Array("/more"))
  def morePage(model: Model): String = {
    model.addAttribute("admin", SessionUtil.getUser().admin)
    "more"
  }

  @RequestMapping(value = Array("/historyData"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @ResponseBody
  def loadHistoryData(): ResponseEntity[java.util.List[UiBook]] = {
    val progressWithBooks: Seq[(Progress, Option[Book])] = bookService.loadAllReadInformation()

    val latestRead = progressWithBooks
      .map(pb => {
        val p = pb._1
        val book = pb._2
        UiBook(
          p.bookId,
          if (book.isDefined) getType(book.get) else null,
          p.collection,
          p.title,
          null,
          p.position,
          p.size,
          p.lastUpdate,
          book.isEmpty
        )
      }).sortBy(_.lastUpdate).reverse

    new ResponseEntity[java.util.List[UiBook]](latestRead.asJava, HttpStatus.OK)
  }

  @RequestMapping(value = Array("/latestRead"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @ResponseBody
  def loadLatestRead(@RequestParam(name = "limit", required = true) limit: Integer): ResponseEntity[java.util.List[UiBook]] = {
    val progress = bookService.loadValidTopProgress(limit)
    val books: Map[String, Book] = progress.map(p => p.bookId).distinct.flatMap(id => bookService.loadBook(id)).map(b => (b.id, b)).toMap

    val latestRead = progress
      .map(p => {
        val book = books(p.bookId)
        UiBook(
          p.bookId,
          getType(book),
          book.collection,
          book.title,
          WebUtil.toBase64Image(book.mediaType, book.cover),
          p.position,
          book.size,
          p.lastUpdate
        )
      }).sortBy(_.lastUpdate).reverse

    new ResponseEntity[java.util.List[UiBook]](latestRead.asJava, HttpStatus.OK)
  }

  @RequestMapping(value = Array("/latestAdded"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @ResponseBody
  def loadLatestAdded(@RequestParam(name = "limit", required = false) limit: Integer,
                     @RequestParam(name = "withoutImages", required = false) withoutImages: Boolean): ResponseEntity[java.util.List[UiBook]] = {
    val books = bookService.loadLatestAdded(limit)
    val progress: Seq[Progress] = bookService.loadProgress(books)
    val progressByBook: Map[String, Progress] = progress.map(p => (p.bookId, p)).toMap

    val uiBooks = books
      .map(book => UiBook(
        book.id,
        getType(book),
        book.collection,
        book.title,
        WebUtil.toBase64Image(book.mediaType, book.cover),
        progressByBook.get(book.id).map(p => p.position).getOrElse(-1),
        progressByBook.get(book.id).map(p => book.size).getOrElse(-1),
        progressByBook.get(book.id).map(p => p.lastUpdate).getOrElse(null)
      ))
    new ResponseEntity[java.util.List[UiBook]](uiBooks.asJava, HttpStatus.OK)
  }

  @RequestMapping(value = Array("/search"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @ResponseBody
  def search(@RequestParam("term") term: String, @RequestParam("page") page: Int): ResponseEntity[CollectionPage] = {
    val books: Seq[Book] = if (term.isEmpty) bookService.getCollectionPage(page)
      else bookService.search(term, page)

    val progress: Seq[Progress] = bookService.loadProgress(books)
    val progressByBook: Map[String, Progress] = progress.map(p => (p.bookId, p)).toMap

    val collections = books.map(c => c.collection).distinct.sortBy(c => c.toLowerCase())
    val uiBooks = books
      .map(book => UiBook(
        book.id,
        getType(book),
        book.collection,
        book.title,
        WebUtil.toBase64Image(book.mediaType, book.cover),
        progressByBook.get(book.id).map(p => p.position).getOrElse(-1),
        book.size,
        progressByBook.get(book.id).map(p => p.lastUpdate).getOrElse(null)
      ))
    new ResponseEntity[CollectionPage](CollectionPage(collections.asJava, uiBooks.asJava), HttpStatus.OK)
  }

  def getType(book: Book): String = {
    book.fileType match {
      case FileTypes.CBR => "comic"
      case FileTypes.CBZ => "comic"
      case FileTypes.PDF => "comic"
      case FileTypes.EPUB => "book"
    }
  }

  @RequestMapping(Array("/"))
  def loadMainPage(): String = "library"

  @RequestMapping(
    value=Array("/removeProgress"),
    method=Array(RequestMethod.DELETE)
  )
  def removeProgress(@RequestParam("id") id: String): ResponseEntity[String] = {
    if (bookService.deleteProgress(id)) new ResponseEntity[String](HttpStatus.OK)
    else new ResponseEntity[String](HttpStatus.NOT_FOUND)
  }

  @RequestMapping(value=Array("/markProgress"), method=Array(RequestMethod.PUT))
  def markProgress(@RequestParam("id") id: String,
                   @RequestParam("position") position: Int): ResponseEntity[String] = {
    if (bookService.saveProgress(id, position)) new ResponseEntity[String](HttpStatus.OK)
    else new ResponseEntity[String](HttpStatus.NOT_FOUND)
  }

  @RequestMapping(value=Array("/loadProgress"), method=Array(RequestMethod.GET))
  def loadProgress(@RequestParam("id") id: String): ResponseEntity[java.lang.Long] = {
    bookService.loadProgress(id).map(p => new ResponseEntity[java.lang.Long](p.position, HttpStatus.OK))
      .getOrElse(new ResponseEntity[java.lang.Long](0L, HttpStatus.OK))
  }
}

case class CollectionPage(
                           @BeanProperty collections: java.util.List[String],
                           @BeanProperty books: java.util.List[UiBook]
                         )

case class UiBook(
                    @BeanProperty id: java.lang.String,
                    @BeanProperty `type`: String,
                    @BeanProperty collection: String,
                    @BeanProperty title: String,
                    @BeanProperty cover: String,
                    @BeanProperty progress: Int,
                    @BeanProperty pages: Int,
                    @BeanProperty lastUpdate: Date,
                    @BeanProperty orphaned: Boolean = false
                  )
