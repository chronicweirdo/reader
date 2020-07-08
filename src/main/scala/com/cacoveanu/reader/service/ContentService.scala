package com.cacoveanu.reader.service

import com.cacoveanu.reader.entity.{Book, Content, Setting}
import com.cacoveanu.reader.repository.{BookRepository, SettingRepository}
import com.cacoveanu.reader.service.xml.{LinkRewriteRule, MetaAppendRule, ResilientXmlLoader, ResourceAppendRule}
import com.cacoveanu.reader.util.{CbrUtil, CbzUtil, EpubUtil, FileMediaTypes, FileTypes, FileUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional

import scala.xml.transform.RuleTransformer

@Service
class ContentService {

  private val COMIC_PART_SIZE = 20

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  @BeanProperty
  @Autowired
  var settingService: SettingService = _

  @Cacheable(Array("comicParts"))
  def loadComicPart(id: String, part: Int): Seq[Content] = {
    bookRepository.findById(id).asScala match {
      case Some(book) =>
        FileUtil.getExtension(book.path) match {
          case FileTypes.CBR => CbrUtil.readPages(book.path, Some(computePagesForPart(part))).getOrElse(Seq())
          case FileTypes.CBZ => CbzUtil.readPages(book.path, Some(computePagesForPart(part))).getOrElse(Seq())
          case _ => Seq()
        }
      case None => Seq()
    }
  }

  def loadResource(bookId: String, resourcePath: String): Option[Content] = {
    bookRepository.findById(bookId).asScala
        .flatMap(book => FileUtil.getExtension(book.path) match {
          case FileTypes.EPUB =>
            EpubUtil.readResource(book.path, resourcePath).flatMap(bytes => processResource(book, resourcePath, bytes))
          case FileTypes.CBR =>
            CbrUtil.readResource(book.path, resourcePath)
          case FileTypes.CBZ =>
            CbzUtil.readResource(book.path, resourcePath)
          case _ =>
            None
        })
  }

  def loadResource(bookId: String, position: Int): Option[Content] = {
    bookRepository.findById(bookId).asScala
      .flatMap(book => FileUtil.getExtension(book.path) match {
        case FileTypes.EPUB =>
          book.toc.asScala.find(e => e.start + e.size >= position)
            .flatMap(tocEntry => {
              val baseLink = EpubUtil.baseLink(tocEntry.link)
              val r: Option[Content] = EpubUtil.readResource(book.path, baseLink)
                .flatMap(bytes => processResource(book, baseLink, bytes))
              r
            })
        case FileTypes.CBZ =>
          CbzUtil.readPages(book.path, Some(Seq(position))).flatMap(c => c.headOption)
        case FileTypes.CBR =>
          CbrUtil.readPages(book.path, Some(Seq(position))).flatMap(c => c.headOption)
      })
  }

  private def processResource(book: Book, resourcePath: String, bytes: Array[Byte]) = {
    FileUtil.getMediaType(resourcePath) match {

      case Some(FileMediaTypes.TEXT_HTML_VALUE) =>
        val toc = book.toc.asScala.zipWithIndex
        val currentToc = toc.find(e => EpubUtil.baseLink(e._1.link) == EpubUtil.baseLink(resourcePath))
        val currentIndex = currentToc.map(_._2)
        val prev = currentIndex.flatMap(i => toc.find(_._2 == i - 1)).map(_._1.link)
        val next = currentIndex.flatMap(i => toc.find(_._2 == i + 1)).map(_._1.link)
        val size = currentToc.map(e => e._1.size).getOrElse(1)
        val sizeUntilNow = currentToc.map(e => e._1.start).getOrElse(0)

        val bookZoom = settingService.getSetting(Setting.BOOK_ZOOM)

        val data: Array[Byte] = processHtml(
          book.id,
          resourcePath,
          new String(bytes, "UTF-8"),
          prev,
          next,
          size,
          sizeUntilNow,
          book.title,
          book.size,
          book.collection,
          bookZoom
        ).getBytes("UTF-8")

        Some(Content(None, FileMediaTypes.TEXT_HTML_VALUE, data))

      case Some(contentType) => Some(Content(None, contentType, bytes))

      case _ => None
    }
  }

  def computePartNumberForPage(page: Int) = {
    page / COMIC_PART_SIZE
  }

  private def computePagesForPart(part: Int) = {
    (part * COMIC_PART_SIZE) until (part * COMIC_PART_SIZE + COMIC_PART_SIZE)
  }

  private def processHtml(
                           bookId: String,
                           path: String,
                           htmlContent: String,
                           previousSection: Option[String],
                           nextSection: Option[String],
                           sectionSize: Int,
                           sectionStart: Int,
                           bookTitle: String,
                           bookSize: Int,
                           collection: String,
                           bookZoom: String
                         ): String = {

    val linkRewriteRule = new LinkRewriteRule(bookId, path)
    val resourceAppendRule = new ResourceAppendRule(Seq(
      "js" -> "/reader.js",
      "js" -> "/hammer.min.js",
      "css" -> "/reader.css"
    ))
    val metaAppendRule = new MetaAppendRule(Map(
      "nextSection" -> nextSection.getOrElse(""),
      "prevSection" -> previousSection.getOrElse(""),
      "bookId" -> bookId,
      "sectionSize" -> sectionSize.toString,
      "sectionStart" -> sectionStart.toString,
      "title" -> bookTitle,
      "bookSize" -> bookSize.toString,
      "collection" -> collection,
      "bookZoom" -> bookZoom
    ))

    new RuleTransformer(
      linkRewriteRule,
      resourceAppendRule,
      metaAppendRule
    )
      .transform(ResilientXmlLoader.loadString(htmlContent))
      .head
      .toString()
  }
}
