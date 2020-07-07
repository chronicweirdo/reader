package com.cacoveanu.reader.service

import com.cacoveanu.reader.entity.{Book, Content}
import com.cacoveanu.reader.repository.BookRepository
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
    bookRepository.findById(bookId).asScala match {
      case Some(book) =>
        EpubUtil.readResource(book.path, resourcePath) match {
          case Some(bytes) => processResource(book, resourcePath, bytes)
          case None => None
        }
      case None => None
    }
  }

  private def processResource(book: Book, resourcePath: String, bytes: Array[Byte]) = {
    FileUtil.getMediaType(resourcePath) match {

      case Some(FileMediaTypes.TEXT_HTML_VALUE) =>
        val toc = EpubUtil.getToc(book.path).zipWithIndex
        val currentToc = toc.find(e => EpubUtil.baseLink(e._1.link) == EpubUtil.baseLink(resourcePath))
        val currentIndex = currentToc.map(_._2)
        val prev = currentIndex.flatMap(i => toc.find(_._2 == i - 1)).map(_._1.link)
        val next = currentIndex.flatMap(i => toc.find(_._2 == i + 1)).map(_._1.link)
        val size = currentToc.map(e => e._1.size).getOrElse(1)
        val sizeUntilNow = currentIndex.map(i =>
          toc.filter(e => e._2 < i).map(e => e._1.size).sum
        ).getOrElse(0)

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
                           collection: String
                         ): String = {

    val linkRewriteRule = new LinkRewriteRule(bookId, path)
    val resourceAppendRule = new ResourceAppendRule(Map("js" -> "/reader.js", "css" -> "/reader.css"))
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
