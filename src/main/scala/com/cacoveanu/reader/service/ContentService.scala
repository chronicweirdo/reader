package com.cacoveanu.reader.service

import com.cacoveanu.reader.entity.{Book, Content}
import com.cacoveanu.reader.repository.BookRepository
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional
import com.cacoveanu.reader.util._
import com.cacoveanu.reader.util.HtmlUtil.AugmentedHtmlString
import com.cacoveanu.reader.util.HtmlUtil.AugmentedJsoupDocument
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@Service
class ContentService {

  private val BATCH_SIZE = 20

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  //@Cacheable(Array("resource"))
  def loadResource(bookId: String, resourcePath: String): Option[Content] = {
    bookRepository.findById(bookId).asScala
        .flatMap(book => FileUtil.getExtension(book.path) match {
          case FileTypes.EPUB if resourcePath == "toc" =>
            processResource(book, "toc.html", getBookTocHtml(book).getBytes)
          case FileTypes.EPUB =>
            val basePath = EpubUtil.baseLink(resourcePath)
            EpubUtil.readResource(book.path, basePath).flatMap(bytes => processResource(book, basePath, bytes))
          case FileTypes.CBR =>
            CbrUtil.readResource(book.path, resourcePath)
          case FileTypes.CBZ =>
            CbzUtil.readResource(book.path, resourcePath)
          case _ =>
            None
        })
  }

  private def getBookTocHtml(book: Book) = {
    (<html>
      <head>
        <title>{book.title}</title>
      </head>
      <body>
        {book.toc.asScala.map(e => {
        <p><a href={e.link}>{e.title}</a></p>
      })}
      </body>
    </html>).toString()
  }

  private def findResourceByPosition(book: Book, position: Int) = {
    book.getSections().find(e => e.start + e.size >= position)
      .map(tocEntry => EpubUtil.baseLink(tocEntry.link))
  }

  @Cacheable(Array("resources"))
  def loadResources(bookId: String, positions: Seq[Int]): Seq[Content] = {
    bookRepository.findById(bookId).asScala match {
      case Some(book) => FileUtil.getExtension(book.path) match {
        /*case FileTypes.EPUB =>
          positions
            .flatMap(p => findResourceByPosition(book, p))
            .distinct
            .flatMap(baseLink =>
              EpubUtil.readResource(book.path, baseLink)
                .flatMap(bytes => processResource(book, baseLink, bytes))
            )*/

        case FileTypes.CBZ =>
          CbzUtil.readPages(book.path, Some(positions)).getOrElse(Seq())

        case FileTypes.CBR =>
          CbrUtil.readPages(book.path, Some(positions)).getOrElse(Seq())

        case _ =>
          Seq()
      }

      case None =>
        Seq()
    }
  }

  private def processResource(book: Book, resourcePath: String, bytes: Array[Byte]) = {
    FileUtil.getMediaType(resourcePath) match {
      case Some(FileMediaTypes.TEXT_HTML_VALUE) =>
        Some(Content(None, FileMediaTypes.TEXT_HTML_VALUE, processHtml(book, resourcePath, bytes)))

      case Some(contentType) =>
        Some(Content(None, contentType, bytes))

      case _ => None
    }
  }

  def getBatchForPosition(position: Int): Seq[Int] = {
    val part = position / BATCH_SIZE
    val positions = (part * BATCH_SIZE) until (part * BATCH_SIZE + BATCH_SIZE)
    positions
  }

  private def processHtml(book: Book, resourcePath: String, bytes: Array[Byte]) = {
    val sections = book.getSections().zipWithIndex
    val currentPath = EpubUtil.baseLink(resourcePath)
    val currentSection = sections.find(e => e._1.link == currentPath)
    val currentIndex = currentSection.map(_._2)
    val prev = currentIndex.flatMap(i => sections.find(_._2 == i - 1)).map(_._1.link).getOrElse("")
    val next = currentIndex.flatMap(i => sections.find(_._2 == i + 1)).map(_._1.link).getOrElse("")
    val size = currentSection.map(e => e._1.size).getOrElse(1)
    val start = currentSection.map(e => e._1.start).getOrElse(0)

    val htmlContent = new String(bytes, "UTF-8")

    htmlContent
      .asHtml
      .transformLinks(resourcePath, book.id)
      .addResources(Seq(
        "js" -> "/reader.js",
        "js" -> "/hammer.min.js",
        "css" -> "/reader.css"
      ))
      .addMeta(Map(
        "nextSection" -> next,
        "prevSection" -> prev,
        "currentSection" -> currentPath,
        "bookId" -> book.id,
        "sectionSize" -> size.toString,
        "sectionStart" -> start.toString,
        "title" -> book.title,
        "bookSize" -> book.size.toString,
        "collection" -> book.collection//,
        //"tocLink" -> book.tocLink
      ))
      .asString
      .getBytes("UTF-8")
  }
}
