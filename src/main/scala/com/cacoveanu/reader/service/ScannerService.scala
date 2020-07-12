package com.cacoveanu.reader.service

import java.nio.file.Paths

import com.cacoveanu.reader.entity.Book
import com.cacoveanu.reader.repository.BookRepository
import com.cacoveanu.reader.util.{CbrUtil, CbzUtil, EpubUtil, FileTypes, FileUtil}
import javax.annotation.PostConstruct
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service

import scala.jdk.CollectionConverters._
import scala.beans.BeanProperty

@Service
class ScannerService {

  private val log: Logger = LoggerFactory.getLogger(classOf[ScannerService])
  private val SUPPORTED_FILES_REGEX = s".+\\.(${FileTypes.CBR}|${FileTypes.CBZ}|${FileTypes.EPUB})$$"
  private val COVER_RESIZE_MINIMAL_SIDE = 500

  @Value("${library.location}")
  @BeanProperty
  var libraryLocation: String = _

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  @BeanProperty
  @Autowired var imageService: ImageService = _

  @PostConstruct
  def updateLibrary() = scan()

  def scan() = {
    val books = FileUtil.scanFilesRegexWithChecksum(libraryLocation, SUPPORTED_FILES_REGEX)
      .flatMap { case (checksum, path) => scanFile(checksum, path) }
    bookRepository.saveAll(books.asJava)
  }

  private def scanFile(checksum: String, path: String): Option[Book] = {
    FileUtil.getExtension(path) match {
      case FileTypes.CBR => scanCbr(checksum, path)
      case FileTypes.CBZ => scanCbz(checksum, path)
      case FileTypes.EPUB => scanEpub(checksum, path)
      case _ => None
    }
  }

  private def getCollection(path: String): String = {
    val pathObject = Paths.get(path)
    val collectionPath = Paths.get(libraryLocation).relativize(pathObject.getParent)
    collectionPath.toString
  }

  private def scanCbr(checksum: String, path: String): Option[Book] = {
    val id = checksum
    val title = FileUtil.getFileName(path)
    val author = ""
    val collection = getCollection(path)
    val cover = CbrUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
    val size = CbrUtil.countPages(path)
    (cover, size) match {
      case (Some(c), Some(s)) =>
        val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
        Some(new Book(id, path, title, author, collection, c.mediaType, smallerCover, s))
      case _ =>
        log.warn(s"failed to scan $path")
        None
    }
  }

  private def scanCbz(checksum: String, path: String): Option[Book] = {
    val id = checksum
    val title = FileUtil.getFileName(path)
    val author = ""
    val collection = getCollection(path)
    val cover = CbzUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
    val size = CbzUtil.countPages(path)
    (cover, size) match {
      case (Some(c), Some(s)) =>
        val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
        Some(new Book(id, path, title, author, collection, c.mediaType, smallerCover, s))
      case _ =>
        log.warn(s"failed to scan $path")
        None
    }
  }

  private[service] def scanEpub(checksum: String, path: String): Option[Book] = {
    val id = checksum
    val title = EpubUtil.getTitle(path).getOrElse(FileUtil.getFileName(path))
    val author = EpubUtil.getAuthor(path).getOrElse("")
    val collection = getCollection(path)
    val cover = EpubUtil.getCover(path)
    val toc = EpubUtil.getToc(path)
    val tocLink = EpubUtil.getTocLink(path)
    val sections = EpubUtil.getSections(path, toc)
    val size = sections.lastOption.map(e => e.start + e.size).getOrElse(0)
    cover match {
      case Some(c) =>
        val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
        val book = new Book(id, path, title, author, collection, c.mediaType, smallerCover, size)
        book.sections = sections.asJava
        tocLink.foreach(s => book.tocLink = s)
        Some(book)
      case _ =>
        log.warn(s"failed to scan $path")
        None
    }
  }
}
