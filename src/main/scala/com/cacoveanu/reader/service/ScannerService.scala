package com.cacoveanu.reader.service

import java.nio.file.Paths

import com.cacoveanu.reader.entity.{Book, Progress}
import com.cacoveanu.reader.repository.{BookRepository, ProgressRepository}
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
  @Autowired
  var progressRepository: ProgressRepository = _

  @BeanProperty
  @Autowired var imageService: ImageService = _

  @PostConstruct
  def updateLibrary() = scan()

  @Scheduled(cron = "0 0 */3 * * *")
  def scheduledRescan() = scan()

  private implicit val executionContext = ExecutionContext.global

  def scan() = Future {
    log.info("scanning library")
    val t1 = System.currentTimeMillis()

    val filesOnDisk = FileUtil.scanFilesRegex(libraryLocation, SUPPORTED_FILES_REGEX).toSet
    val filesInDatabase = bookRepository.findAllPaths().asScala.toSet

    val newFiles = filesOnDisk.diff(filesInDatabase)
    val deletedFiles = filesInDatabase.diff(filesOnDisk)

    val t2 = System.currentTimeMillis()
    log.info(s"discovering files on disk took ${t2 - t1} milliseconds")
    val newBooks: Seq[Book] = newFiles
      .toSeq
      .toBatches(DB_BATCH_SIZE)
      .flatMap(batch => {
        val books = batch.flatMap(path => scanFile(path))
        bookRepository.saveAll(books.asJava).asScala
      })
    val t3 = System.currentTimeMillis()
    log.info(s"scanning and saving files took ${t3 - t2} milliseconds")
    val toDelete = bookRepository.findByPathIn(deletedFiles.toSeq.asJava)
    val toDeleteProgress = progressRepository.findByBookIn(toDelete).asScala
    val matchedProgress = toDeleteProgress.flatMap(p =>
      findEquivalent(p.book, newBooks)
        .map(newBook => new Progress(p.user, newBook, p.section, p.position, p.lastUpdate, p.finished))
    )
    progressRepository.saveAll(matchedProgress.asJava)
    bookRepository.deleteAll(toDelete)
    val t4 = System.currentTimeMillis()
    log.info(s"deleting missing files took ${t4 - t3} milliseconds")
    log.info(s"full scan done, took ${t4 - t1} milliseconds")
  }

  private def findEquivalent(oldBook: Book, newBooks: Seq[Book]) = {
    val candidates = newBooks.filter(b => stringsAlike(b.title, oldBook.title) && stringsAlike(b.author, oldBook.author))
    if (candidates.size == 1) {
      Some(candidates.head)
    } else {
      None
    }
  }

  private def stringsAlike(s1: String, s2: String) = {
    s1 == s2
  }

  private def scanFile(path: String): Option[Book] = {
    FileUtil.getExtension(path) match {
      case FileTypes.CBR => scanCbr(path)
      case FileTypes.CBZ => scanCbz(path)
      case FileTypes.EPUB => scanEpub(path)
      case _ => None
    }
  }

  private def getCollection(path: String): String = {
    val pathObject = Paths.get(path)
    val collectionPath = Paths.get(libraryLocation).relativize(pathObject.getParent)
    collectionPath.toString
  }

  private def scanCbr(path: String): Option[Book] = {
    val title = FileUtil.getFileName(path)
    val author = ""
    val collection = getCollection(path)
    val cover = CbrUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
    val size = CbrUtil.countPages(path)
    (cover, size) match {
      case (Some(c), Some(s)) =>
        val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
        Some(new Book(path, title, author, collection, c.mediaType, smallerCover, s))
      case _ =>
        log.warn(s"failed to scan $path")
        None
    }
  }

  private def scanCbz(path: String): Option[Book] = {
    val title = FileUtil.getFileName(path)
    val author = ""
    val collection = getCollection(path)
    val cover = CbzUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
    val size = CbzUtil.countPages(path)
    (cover, size) match {
      case (Some(c), Some(s)) =>
        val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
        Some(new Book(path, title, author, collection, c.mediaType, smallerCover, s))
      case _ =>
        log.warn(s"failed to scan $path")
        None
    }
  }

  private[service] def scanEpub(path: String): Option[Book] = {
    val title = EpubUtil.getTitle(path).getOrElse(FileUtil.getFileName(path))
    val author = EpubUtil.getAuthor(path).getOrElse("")
    val collection = getCollection(path)
    val cover = EpubUtil.getCover(path)
    val toc = EpubUtil.getToc(path)
    val size = EpubUtil.getSections(toc).lastOption.map(e => e.start + e.size).getOrElse(0)
    cover match {
      case Some(c) =>
        val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
        val book = new Book(path, title, author, collection, c.mediaType, smallerCover, size)
        book.toc = toc.asJava
        Some(book)
      case _ =>
        log.warn(s"failed to scan $path")
        None
    }
  }
}
