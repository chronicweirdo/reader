package com.cacoveanu.reader.service

import java.lang
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import com.cacoveanu.reader.entity.{Book, Content}
import com.cacoveanu.reader.repository.BookRepository
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional
import com.cacoveanu.reader.util._
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

import scala.None.orNull
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

@Service
class ContentService {

  private val BATCH_SIZE = 20

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  @Value("${keepEbookStyles}")
  @BeanProperty
  var keepEbookStyles: Boolean = _

  //@Cacheable(Array("resource"))
  def loadResource(bookId: java.lang.Long, resourcePath: String): Option[Content] = {
    bookRepository.findById(bookId).asScala
        .flatMap(book => FileUtil.getExtension(book.path) match {
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

  @Cacheable(Array("sectionStartPosition"))
  def findStartPositionForSectionContaining(bookId: java.lang.Long, position: java.lang.Long): Long = {
    bookRepository.findById(bookId).asScala match {
      case Some(book) if FileUtil.getExtension(book.path) == FileTypes.EPUB =>
        book.resources.asScala.find(r => r.start <= position && position <= r.end) match {
          case Some(section) => section.start
          case None => -1
        }
      case _ => -1
    }
  }

  private def getFolderPath(resourcePath: String): String = {
    val contextPath = resourcePath
    val lio = contextPath.lastIndexOf("/")
    if (lio > 0) contextPath.substring(0, lio)
    else ""
  }

  private def splitPaths(src: String): (String, String) = {
    val hashIndex = src.lastIndexOf("#")
    if (hashIndex > 0) (src.substring(0, hashIndex), src.substring(hashIndex+1))
    else (src, null)
  }

  private def imageLinkTransform(bookId: Long, resourcePath: String, oldSrc: String): String = {
    val remoteUri = new URI(oldSrc)
    if (remoteUri.isAbsolute) {
      oldSrc
    } else {
      val (externalPath, internalPath) = splitPaths(oldSrc)
      val folder = getFolderPath(resourcePath)
      val remotePathWithFolder = if (folder.length > 0) folder + "/" + externalPath else externalPath
      val normalizedPath = Paths.get(remotePathWithFolder).normalize().toString.replaceAll("\\\\", "/")
      s"bookResource?id=$bookId&path=${URLEncoder.encode(normalizedPath, "UTF-8")}" + (if (internalPath != null) "#" + internalPath else "")
    }
  }

  private def hrefLinkTransform(linksMap: Map[String, Long], resourcePath: String, oldHref: String): (String, String) = {
    val remoteUri = new URI(oldHref)
    val folder = getFolderPath(resourcePath)
    if (remoteUri.isAbsolute) {
      ("href", oldHref)
    } else if (linksMap.contains(oldHref)) {
      ("onclick", s"displayPageFor(${linksMap(oldHref)})")
    } else if (linksMap.contains(folder + "/" + oldHref)) {
      ("onclick", s"displayPageFor(${linksMap(folder + "/" + oldHref)})")
    } else {
      ("href", "")
    }
  }

  private def nodeSrcTransform(bookId: Long, resourcePath: String, node: BookNode): BookNode = {
    node.srcTransform(imageLinkTransform(bookId, resourcePath, _: String))
    node
  }

  private def nodeHrefTransform(linksMap: Map[String, Long], resourcePath: String, node: BookNode): BookNode = {
    node.hrefTransform(hrefLinkTransform(linksMap, resourcePath, _: String))
    node
  }

  @Cacheable(Array("bookSection"))
  def loadBookSection(bookId: java.lang.Long, position: Long): BookNode = {
    bookRepository.findById(bookId).asScala match {
      case Some(book) if FileUtil.getExtension(book.path) == FileTypes.EPUB => {
        book.resources.asScala.find(r => r.start <= position && position <= r.end)
          .map(resource => (resource.path, resource.start))
          .flatMap { case (resourcePath, resourceStart) => {
            EpubUtil.parseSection(book.path, resourcePath, resourceStart)
              .map(nodeSrcTransform(bookId, resourcePath, _))
              .map(nodeHrefTransform(book.links.asScala.map(l => (l.link -> l.position.toLong)).toMap, resourcePath, _))
          }}.orNull
      }
      case _ => null
    }
  }

  @Cacheable(Array("resources"))
  def loadResources(bookId: java.lang.Long, positions: Seq[Int]): Seq[Content] = {
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

        case FileTypes.PDF =>
          PdfUtil.readPages(book.path, Some(positions)).getOrElse(Seq())

        case _ =>
          Seq()
      }

      case None =>
        Seq()
    }
  }

  private def processResource(book: Book, resourcePath: String, bytes: Array[Byte]) = {
    FileUtil.getMediaType(resourcePath) match {
      /*case Some(FileMediaTypes.TEXT_HTML_VALUE) =>
        Some(Content(None, FileMediaTypes.TEXT_HTML_VALUE, processHtml(book, resourcePath, bytes)))*/

      case Some(contentType) =>
        Some(Content(None, contentType, bytes))

      /*case None if resourcePath == "toc" =>
        Some(Content(None, FileMediaTypes.TEXT_HTML_VALUE, processHtml(book, resourcePath, bytes)))*/

      case _ => None
    }
  }

  def getBatchForPosition(position: Int): Seq[Int] = {
    val part = position / BATCH_SIZE
    val positions = (part * BATCH_SIZE) until (part * BATCH_SIZE + BATCH_SIZE)
    positions
  }
}
