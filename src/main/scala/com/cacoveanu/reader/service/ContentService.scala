package com.cacoveanu.reader.service

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

  @Cacheable(Array("bookSection"))
  def loadBookSection(bookId: java.lang.Long, position: Long): BookNode = {
    bookRepository.findById(bookId).asScala match {
      case Some(book) if FileUtil.getExtension(book.path) == FileTypes.EPUB => {
        // find resource for position
        val resource = book.resources.asScala.find(r => r.start <= position && position <= r.end).get
        val node = EpubUtil.parseSection(book.path, resource.path, resource.start).get
        // fix image links
        def imageLinkTransform(oldSrc: String): String = {
          val remoteUri = new URI(oldSrc)
          if (remoteUri.isAbsolute) {
            return oldSrc
          } else {
            val di = oldSrc.lastIndexOf("#")
            val (externalPath, internalPath) = if (di > 0) (oldSrc.substring(0, di), oldSrc.substring(di+1))
            else (oldSrc, null)
            val contextPath = resource.path
            val lio = contextPath.lastIndexOf("/")
            val folder = if (lio > 0) contextPath.substring(0, lio)
            else ""
            val remotePathWithFolder = if (folder.length > 0) folder + "/" + externalPath else externalPath
            val normalizedPath = Paths.get(remotePathWithFolder).normalize().toString.replaceAll("\\\\", "/")
            return s"bookResource?id=$bookId&path=${URLEncoder.encode(normalizedPath, "UTF-8")}" + (if (internalPath != null) "#" + internalPath else "")
          }
        }
        node.srcTransform(imageLinkTransform)

        val linksMap = book.links.asScala.map(l => (l.link -> l.position)).toMap
        def hrefLinkTransform(oldHref: String): (String, String) = {
          val contextPath = resource.path
          val lio = contextPath.lastIndexOf("/")
          val folder = if (lio > 0) contextPath.substring(0, lio)
          else ""
          if (linksMap.contains(oldHref)) {
            val position: Long = linksMap(oldHref)
            return ("onclick", s"displayPageFor($position)")
          } else if (linksMap.contains(folder + "/" + oldHref)) {
            val position: Long = linksMap(folder + "/" + oldHref)
            return ("onclick", s"displayPageFor($position)")
          } else {
            ("href", oldHref)
          }
        }
        node.hrefTransform(hrefLinkTransform)

        node
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
