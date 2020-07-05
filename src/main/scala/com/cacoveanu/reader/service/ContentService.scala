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

import scala.collection.immutable
import scala.xml.factory.XMLLoader
import scala.xml.{Elem, Node, SAXParser, XML}
import scala.xml.transform.{RewriteRule, RuleTransformer}

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
        val currentIndex: Option[Int] = toc.find(e => EpubUtil.baseLink(e._1.link) == EpubUtil.baseLink(resourcePath)).map(_._2)
        val prev = currentIndex.flatMap(i => toc.find(_._2 == i - 1)).map(_._1.link)
        val next = currentIndex.flatMap(i => toc.find(_._2 == i + 1)).map(_._1.link)

        val data: Array[Byte] = processHtml(book.id, resourcePath, new String(bytes, "UTF-8"), prev, next)
          .getBytes("UTF-8")
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
                           nextSection: Option[String]
                         ): String = {

    val linkRewriteRule = new LinkRewriteRule(bookId, path)
    val resourceAppendRule = new ResourceAppendRule(Map("js" -> "/reader.js", "css" -> "/reader.css"))
    val metaAppendRule = new MetaAppendRule(Map(
      "nextSection" -> nextSection.getOrElse(""),
      "prevSection" -> previousSection.getOrElse(""),
      "bookId" -> bookId
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

/*
object MyXML extends XMLLoader[Elem] {
  override def parser: SAXParser = {
    val f = javax.xml.parsers.SAXParserFactory.newInstance()
    //f.setNamespaceAware(false)
    //f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    f.setValidating(false)
    f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    f.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
    f.newSAXParser()
  }
}*/
