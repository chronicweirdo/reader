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

  private val BATCH_SIZE = 20

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  // todo: the caches here interfere with settings load, settings should be loaded separately from the book metadata
  @Cacheable(Array("resource"))
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

  private def findResourceByPosition(book: Book, position: Int) = {
    book.toc.asScala.find(e => e.start + e.size >= position)
      .map(tocEntry => EpubUtil.baseLink(tocEntry.link))
  }

  @Cacheable(Array("resources"))
  def loadResources(bookId: String, positions: Seq[Int]): Seq[Content] = {
    bookRepository.findById(bookId).asScala match {
      case Some(book) => FileUtil.getExtension(book.path) match {
        case FileTypes.EPUB =>
          positions
            .flatMap(p => findResourceByPosition(book, p))
            .distinct
            .flatMap(baseLink =>
              EpubUtil.readResource(book.path, baseLink)
                .flatMap(bytes => processResource(book, baseLink, bytes))
            )

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

  // todo: refactor these methods, too large!
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
          book.collection
        ).getBytes("UTF-8")

        Some(Content(None, FileMediaTypes.TEXT_HTML_VALUE, data))

      case Some(contentType) => Some(Content(None, contentType, bytes))

      case _ => None
    }
  }

  def getBatchForPosition(position: Int): Seq[Int] = {
    val part = position / BATCH_SIZE
    val positions = (part * BATCH_SIZE) until (part * BATCH_SIZE + BATCH_SIZE)
    positions
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
                           collection: String
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
      "collection" -> collection
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
