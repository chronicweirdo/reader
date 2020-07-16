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
import com.cacoveanu.reader.util.SeqUtil.AugmentedSeq
import org.springframework.scheduling.annotation.Scheduled

import scala.concurrent.{ExecutionContext, Future}

@Service
class ScannerService {

  private val log: Logger = LoggerFactory.getLogger(classOf[ScannerService])
  private val SUPPORTED_FILES_REGEX = s".+\\.(${FileTypes.CBR}|${FileTypes.CBZ}|${FileTypes.EPUB})$$"
  private val COVER_RESIZE_MINIMAL_SIDE = 500
  private val DB_BATCH_SIZE = 20

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

  @Scheduled(cron = "0 0 */3 * * *")
  def scheduledRescan() = scan()

  private implicit val executionContext = ExecutionContext.global

  private def scan() = Future {
    log.info("scanning library")
    val t1 = System.currentTimeMillis()
    val filesOnDisk = FileUtil.scanFilesRegexWithChecksum(libraryLocation, SUPPORTED_FILES_REGEX)
    val t2 = System.currentTimeMillis()
    log.info(s"discovering files on disk took ${t2 - t1} milliseconds")
    val foundIds = filesOnDisk
      .toSeq
      .toBatches(DB_BATCH_SIZE)
      .flatMap(batch => {
        val books = batch.flatMap { case (checksum, path) => scanFile(checksum, path) }
        bookRepository.saveAll(books.asJava).asScala.map(_.id)
      })
    val t3 = System.currentTimeMillis()
    log.info(s"scanning and saving files took ${t3 - t2} milliseconds")
    val toDelete = bookRepository.findByIdNotIn(foundIds.asJava).asScala
    bookRepository.deleteAll(toDelete.asJava)
    val t4 = System.currentTimeMillis()
    log.info(s"deleting missing files took ${t4 - t3} milliseconds")
    log.info(s"full scan done, took ${t4 - t1} milliseconds")
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
    val size = EpubUtil.getSections(toc).lastOption.map(e => e.start + e.size).getOrElse(0)
    cover match {
      case Some(c) =>
        val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
        val book = new Book(id, path, title, author, collection, c.mediaType, smallerCover, size)
        book.toc = toc.asJava
        Some(book)
      case _ =>
        log.warn(s"failed to scan $path")
        None
    }
  }
}
