package com.cacoveanu.reader.service

import java.nio.file.{Files, Paths}
import com.cacoveanu.reader.entity.{Book, Content, Progress}
import com.cacoveanu.reader.repository.{BookRepository, ProgressRepository}
import com.cacoveanu.reader.util.{CbrUtil, CbzUtil, EpubUtil, FileMediaTypes, FileTypes, FileUtil, PdfUtil}

import javax.annotation.PostConstruct
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service

import scala.jdk.CollectionConverters._
import scala.beans.BeanProperty
import com.cacoveanu.reader.util.SeqUtil.AugmentedSeq
import org.springframework.scheduling.annotation.Scheduled

import java.nio.file.attribute.{BasicFileAttributes, FileTime}
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}

@Service
class ScannerService {

  private val log: Logger = LoggerFactory.getLogger(classOf[ScannerService])
  private val SUPPORTED_FILES_REGEX = s".+\\.(${FileTypes.CBR}|${FileTypes.CBZ}|${FileTypes.EPUB}|${FileTypes.PDF})$$"
  private val COVER_RESIZE_MINIMAL_SIDE = 500
  private val DB_BATCH_SIZE = 20

  @Value("${library.location}")
  @BeanProperty
  var libraryLocation: String = _

  @Value("${getTitleFromMetadata:true}")
  @BeanProperty
  var getTitleFromMetadata: Boolean = _

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

  @Scheduled(cron = "0 */30 * * * *")
  def scheduledRescan() = scan()

  private implicit val executionContext = ExecutionContext.global

  private var lastScanDate: Date = _

  private val scanInProgress: AtomicBoolean = new AtomicBoolean(false)

  private def setLastScanDate() = synchronized {
    lastScanDate = new Date()
  }

  def getLastScanDate(): Date = {
    if (lastScanDate != null) lastScanDate.clone().asInstanceOf[Date]
    else null
  }

  def scan() = Future {
    if (! scanInProgress.get()) {
      scanInProgress.set(true)
      try {
        log.info("scanning library")
        val t1 = System.currentTimeMillis()

        val filesOnDisk = FileUtil.scanFilesRegex(libraryLocation, SUPPORTED_FILES_REGEX).toSet
        val filesInDatabase = bookRepository.findAllPaths().asScala.toSet

        val newFiles = filesOnDisk.diff(filesInDatabase)
        val deletedFiles = filesInDatabase.diff(filesOnDisk)

        val t2 = System.currentTimeMillis()
        log.info(s"discovering files on disk took ${t2 - t1} milliseconds")
        log.debug(s"found ${newFiles.size} new files")
        var scannedNewFiles = 0
        val newBooks: Seq[Book] = newFiles
          .toSeq
          .toBatches(DB_BATCH_SIZE)
          .flatMap(batch => {
            log.debug(s"scanning batch of new books of size ${batch.size}")
            val books = batch.flatMap(path => scanFile(path))
            val savedBooks = bookRepository.saveAll(books.asJava).asScala
            scannedNewFiles += batch.size
            val tc = System.currentTimeMillis()
            log.debug(s"scanned ${scannedNewFiles} files in ${tc - t2} milliseconds, ${newFiles.size - scannedNewFiles} remaining")
            savedBooks
          })
        val t3 = System.currentTimeMillis()
        log.info(s"scanning and saving files took ${t3 - t2} milliseconds")
        val toDelete = bookRepository.findByPathIn(deletedFiles.toSeq.asJava)
        val toDeleteProgress = progressRepository.findByBookIn(toDelete).asScala
        val matchedProgress = toDeleteProgress.flatMap(p =>
          findEquivalent(p.book, newBooks)
            .map(newBook => new Progress(p.user, newBook, p.position, p.lastUpdate, p.finished))
        )
        progressRepository.saveAll(matchedProgress.asJava)
        bookRepository.deleteAll(toDelete)
        val t4 = System.currentTimeMillis()
        log.info(s"deleting missing files took ${t4 - t3} milliseconds")
        log.info(s"full scan done, took ${t4 - t1} milliseconds")
        setLastScanDate()
        scanInProgress.set(false)
      } catch {
        case t: Throwable =>
          t.printStackTrace()
      }
    }
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
    log.debug(s"scanning file $path")
    FileUtil.getExtension(path) match {
      case FileTypes.CBR => scanCbr(path)
      case FileTypes.CBZ => scanCbz(path)
      case FileTypes.EPUB => scanEpub(path)
      case FileTypes.PDF => scanPdf(path)
      case _ => None
    }
  }

  private def getCollection(path: String): String = {
    val pathObject = Paths.get(path)
    val collectionPath = Paths.get(libraryLocation).relativize(pathObject.getParent)
    "/" + collectionPath.toString.replaceAll("\\\\", "/")
  }

  private def getFileCreationDate(path: String): Date = {
    try {
      val pathObject = Paths.get(path)
      val attributes: BasicFileAttributes = Files.readAttributes(pathObject, classOf[BasicFileAttributes])
      val creationTime: FileTime = attributes.creationTime()
      new Date(creationTime.toMillis)
    } catch {
      case _: Throwable => new Date()
    }
  }

  private[service] def scanCbr(path: String): Option[Book] = {
    try {
      val title = FileUtil.getFileName(path)
      val author = ""
      val collection = getCollection(path)
      val cover = CbrUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
      val size = CbrUtil.countPages(path)
      (cover, size) match {
        case (Some(c), Some(s)) =>
          val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
          Some(new Book(path, title, author, collection, c.mediaType, smallerCover, s, getFileCreationDate(path)))
        case _ =>
          log.warn(s"failed to scan $path")
          None
      }
    } catch {
      case t: Throwable =>
        log.warn(s"failed to scan $path", t)
        None
    }
  }

  private[service] def scanPdf(path: String): Option[Book] = {
    try {
      val title = FileUtil.getFileName(path)
      val author = ""
      val collection = getCollection(path)
      val cover = PdfUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
      val size = PdfUtil.countPages(path)
      (cover, size) match {
        case (Some(c), Some(s)) =>
          val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
          Some(new Book(path, title, author, collection, c.mediaType, smallerCover, s, getFileCreationDate(path)))
        case _ =>
          log.warn(s"failed to scan $path")
          None
      }
    } catch {
      case t: Throwable =>
        log.warn(s"failed to scan $path", t)
        None
    }
  }

  private[service] def scanCbz(path: String): Option[Book] = {
    try {
      val title = FileUtil.getFileName(path)
      val author = ""
      val collection = getCollection(path)
      val cover = CbzUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
      val size = CbzUtil.countPages(path)
      (cover, size) match {
        case (Some(c), Some(s)) =>
          val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
          Some(new Book(path, title, author, collection, c.mediaType, smallerCover, s, getFileCreationDate(path)))
        case _ =>
          log.warn(s"failed to scan $path")
          None
      }
    } catch {
      case t: Throwable =>
        log.warn(s"failed to scan $path", t)
        None
    }
  }



  private[service] def scanEpub(path: String): Option[Book] = {
    try {
      val title = if (getTitleFromMetadata) EpubUtil.getTitle(path).getOrElse(FileUtil.getFileName(path)) else FileUtil.getFileName(path)
      val author = EpubUtil.getAuthor(path).getOrElse("")
      val collection = getCollection(path)
      val (resources, links, toc) = EpubUtil.scanContentMetadata(path)
      var cover = EpubUtil.getCoverFromOpf(path)
      if (! cover.isDefined) {
        cover = EpubUtil.findCoverInResource(path, resources.head.path)
      }
      if (! cover.isDefined) {
        cover = Some(Content(None, FileMediaTypes.IMAGE_JPEG_VALUE, imageService.generateCover(title)))
      }
      val size = resources.map(_.end).maxOption.map(_.intValue()).getOrElse(0) - resources.map(_.start).minOption.map(_.intValue()).getOrElse(0) + 1
      cover match {
        case Some(c) =>
          val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
          val book = new Book(path, title, author, collection, c.mediaType, smallerCover, size, getFileCreationDate(path))
          book.toc = toc.asJava
          book.resources = resources.asJava
          book.links = links.asJava
          Some(book)
        case _ =>
          log.warn(s"failed to scan $path")
          None
      }
    } catch {
      case t: Throwable =>
        log.warn(s"failed to scan $path", t)
        None
    }
  }
}
