package com.cacoveanu.reader.service

import java.net.{URI, URLEncoder}
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

  @BeanProperty
  @Autowired
  var imageService: ImageService = _

  @Value("${keepEbookStyles}")
  @BeanProperty
  var keepEbookStyles: Boolean = _

  /**
   * Book resources are loaded based on a path. These are usually images.
   * @param bookId
   * @param resourcePath
   * @return
   */
  @Cacheable(Array("bookResource"))
  def loadBookResource(bookId: java.lang.Long, resourcePath: String): Option[Content] = {
    bookRepository.findById(bookId).asScala
      .filter(book => FileUtil.getExtension(book.path) == FileTypes.EPUB)
      .map(book => (FileUtil.getMediaType(resourcePath), EpubUtil.readResource(book.path, EpubUtil.baseLink(resourcePath))))
      .filter { case (contentTypeOption, bytesOption) => contentTypeOption.isDefined && bytesOption.isDefined}
      .flatMap { case (contentTypeOption, bytesOption) => Some(Content(None, contentTypeOption.get, bytesOption.get))}
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

  private def getAbsolutePath(currentResourcePath: String, oldHref: String): String = {
    if (oldHref.startsWith("/")) oldHref
    else {
      var currentPath = currentResourcePath.split("/").dropRight(1)
      val steps = oldHref.split("/")
      steps.foreach {
        case ".." => currentPath = currentPath.dropRight(1)
        case p => currentPath = currentPath :+ p
      }
      currentPath.mkString("/")
    }
  }

  private def hrefLinkTransform(linksMap: Map[String, Long], resourcePath: String, oldHref: String): (String, String) = {
    val remoteUri = new URI(oldHref)
    if (remoteUri.isAbsolute) {
      ("href", oldHref)
    } else {
      val absoluteOldHref = getAbsolutePath(resourcePath, oldHref)
      if (linksMap.contains(absoluteOldHref)) {
        ("onclick", s"displayPageFor(${linksMap(absoluteOldHref)})")
      } else {
        ("href", "")
      }
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

  /**
   * Comic resources (or PDF), which are images, are loaded in batches of pages.
   * @param bookId
   * @param positions
   * @return
   */
  @Cacheable(Array("resources"))
  def loadComicResources(bookId: java.lang.Long, positions: Seq[Int]): Seq[Content] = {
    val pages = bookRepository.findById(bookId).asScala
      .map(book => (book.path, FileUtil.getExtension(book.path))) match {
        case Some((path, FileTypes.CBZ)) => CbzUtil.readPages(path, Some(positions)).getOrElse(Seq())
        case Some((path, FileTypes.CBR)) => CbrUtil.readPages(path, Some(positions)).getOrElse(Seq())
        case Some((path, FileTypes.PDF)) => PdfUtil.readPages(path, Some(positions)).getOrElse(Seq())
        case _ => Seq()
      }
    pages.map(c => {
      val color = imageService.getDominantEdgeColor(c.data)
      c.copy(c.index, c.mediaType, c.data, Map("color" -> color))
    })
  }

  def getBatchForPosition(position: Int): Seq[Int] = {
    val part = position / BATCH_SIZE
    val positions = (part * BATCH_SIZE) until (part * BATCH_SIZE + BATCH_SIZE)
    positions
  }
}
