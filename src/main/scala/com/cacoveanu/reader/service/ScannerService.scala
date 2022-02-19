package com.cacoveanu.reader.service

import java.nio.file.{FileSystems, Files, Path, Paths, WatchKey, WatchService}
import com.cacoveanu.reader.entity.{Book, Content, Progress}
import com.cacoveanu.reader.repository.{BookRepository, ProgressRepository}
import com.cacoveanu.reader.service.BookFolderChangeType.{ADDED, BookFolderChangeType, DELETED, MODIFIED}
import com.cacoveanu.reader.util.{CbrUtil, CbzUtil, DateUtil, EpubUtil, FileMediaTypes, FileTypes, FileUtil, PdfUtil, ProgressUtil}

import javax.annotation.PostConstruct
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service

import scala.jdk.CollectionConverters._
import scala.beans.BeanProperty
import com.cacoveanu.reader.util.SeqUtil.AugmentedSeq
import org.springframework.scheduling.annotation.Scheduled

import java.nio.file.StandardWatchEventKinds.{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY}
import java.nio.file.attribute.{BasicFileAttributes, FileTime}
import java.util.{Comparator, Date}
import java.util.concurrent.{BlockingQueue, ConcurrentHashMap, LinkedBlockingQueue, PriorityBlockingQueue}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.OptionConverters._


object BookFolderChangeType extends Enumeration {
  type BookFolderChangeType = Value
  val ADDED, MODIFIED, DELETED = Value
}

case class BookFolderChange(path: String, isFile: Boolean, typ: BookFolderChangeType)

class BookFolderChangeComparator extends Comparator[BookFolderChange] {
  override def compare(o1: BookFolderChange, o2: BookFolderChange): Int = (o1.typ, o2.typ) match {
    case (ADDED, _) => -1
    case (DELETED, MODIFIED) => -1
    case _ => 1
  }
}

@Service
class ScannerService {

  private val log: Logger = LoggerFactory.getLogger(classOf[ScannerService])
  private val SUPPORTED_FILES_REGEX = s".+\\.(${FileTypes.CBR}|${FileTypes.CBZ}|${FileTypes.EPUB}|${FileTypes.PDF})$$"
  private val COVER_RESIZE_MINIMAL_SIDE = 500

  @Value("${library.location}")
  @BeanProperty
  var libraryLocation: String = _

  @Value("${getTitleFromMetadata:true}")
  @BeanProperty
  var getTitleFromMetadata: Boolean = _

  @Value("${enableFolderWatching:true}")
  @BeanProperty
  var enableFolderWatching: Boolean = _

  @Value("${verifyOnInitialScan:true}")
  @BeanProperty
  var verifyOnInitialScan: Boolean = _

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  @BeanProperty
  @Autowired
  var progressRepository: ProgressRepository = _

  @BeanProperty
  @Autowired var imageService: ImageService = _

  val scannedFiles = new AtomicLong(0)
  val scanFailures = new AtomicLong(0)
  val scanTimeMilliseconds = new AtomicLong(0)

  var filesSnapshot = Set[String]()

  @PostConstruct
  def init(): Unit = {
    initialScanFolder(libraryLocation)
    if (enableFolderWatching) startWatcher()
    startQueueConsumer()
  }

  private val watchService = FileSystems.getDefault.newWatchService()
  private val watchServiceKeyMap = new ConcurrentHashMap[String, WatchKey]()
  private val changesQueue: BlockingQueue[BookFolderChange] = new PriorityBlockingQueue[BookFolderChange](100, new BookFolderChangeComparator())

  private def startWatching(path: String) = {
    log.debug(s"start watching path $path")
    val key: WatchKey = Paths.get(path).register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    watchServiceKeyMap.put(path, key)
  }

  private def stopWatching(path: String) = {
    log.debug(s"stop watching path $path")
    watchServiceKeyMap.keys().asScala.filter(k => k.startsWith(path)).foreach(k => {
      val key = watchServiceKeyMap.get(k)
      key.cancel()
      watchServiceKeyMap.remove(k)
    })
  }

  private def relativePathToAbsolute(path: String) = {
    Paths.get(libraryLocation, path).toFile.getAbsolutePath
  }

  private def initialScanFolder(path: String) = {
    if (enableFolderWatching) FileUtil.scanFolderTree(path).foreach(f => startWatching(f))

    val filesOnDisk: Set[String] = FileUtil.scanFilesRegex(path, SUPPORTED_FILES_REGEX).toSet
    filesSnapshot = filesOnDisk
    val filesInDatabase = bookRepository.findAllPaths().asScala.map(relativePathToAbsolute(_)).filter(p => p.startsWith(path)).toSet

    val newFiles = filesOnDisk.diff(filesInDatabase)
    log.info(s"found ${newFiles.size} new files in initial scan")
    newFiles.foreach(f => changesQueue.put(BookFolderChange(f, true, ADDED)))

    val deletedFiles = filesInDatabase.diff(filesOnDisk)
    log.info(s"found ${deletedFiles.size} deleted files in initial scan")
    deletedFiles.foreach(f => changesQueue.put(BookFolderChange(f, true, DELETED)))

    if (verifyOnInitialScan) {
      val modifiedFiles = filesInDatabase.diff(deletedFiles)
      log.info(s"found ${modifiedFiles.size} modified files in initial scan")
      modifiedFiles.foreach(f => changesQueue.put(BookFolderChange(f, true, MODIFIED)))
    }
  }

  @Scheduled(cron = "0 */1 * * * *")
  def scheduledRescan() = if (!enableFolderWatching) subsequentScanFolder(libraryLocation)

  private def subsequentScanFolder(path: String) = {
    val filesOnDisk: Set[String] = FileUtil.scanFilesRegex(path, SUPPORTED_FILES_REGEX).toSet
    val newFiles = filesOnDisk.diff(filesSnapshot)
    log.info(s"found ${newFiles.size} new files in subsequent scan")
    newFiles.foreach(f => changesQueue.put(BookFolderChange(f, true, ADDED)))
    val deletedFiles = filesSnapshot.diff(filesOnDisk)
    log.info(s"found ${deletedFiles.size} deleted files in subsequent scan")
    deletedFiles.foreach(f => changesQueue.put(BookFolderChange(f, true, DELETED)))
    filesSnapshot = filesOnDisk
  }

  private def startWatcher() = {
    new Thread(() => {
      while(true) {
        var key: WatchKey = null
        try {
          key = watchService.take()
        } catch {
          case e: Throwable =>
            log.info("watcher service failed to get key", e)
        }

        if (key != null) {
          var changesInEvent: Seq[BookFolderChange] = Seq()
          for (event <- key.pollEvents.asScala) {
            val eventFile = key.watchable().asInstanceOf[Path].resolve(event.context().asInstanceOf[Path]).toFile
            val eventPath = eventFile.getAbsolutePath
            val typ = event.kind() match {
              case ENTRY_CREATE => ADDED
              case ENTRY_MODIFY => MODIFIED
              case ENTRY_DELETE => DELETED
            }
            val change = BookFolderChange(eventPath, eventFile.isFile, typ)
            changesInEvent :+= change
            log.debug(s"detect change $change")
          }
          log.debug(s"found ${changesInEvent.size} changes")
          changesInEvent.foreach(c => changesQueue.put(c))
          key.reset
        }
      }
    }).start()
  }

  private def startQueueConsumer() = {
    new Thread(() => {
      while (true) {
        val change = changesQueue.take()
        change match {
          case BookFolderChange(path, _, DELETED) =>
            log.debug(s"stop following changes in possible folder $path and delete possible book at $path")
            if (enableFolderWatching) stopWatching(path)
            deleteBook(path)
          case BookFolderChange(path, false, ADDED) =>
            log.debug(s"start following changes in folder $path")
            initialScanFolder(path)
          case BookFolderChange(path, true, ADDED) =>
            log.debug(s"scan new book $path")
            verifyAndAddBook(path)
          case BookFolderChange(path, true, MODIFIED) =>
            log.debug(s"rescan and update book $path")
            verifyAndAddBook(path)
          case _ =>
            log.debug("do nothing")
        }
      }
    }).start()
  }

  private def deleteBook(path: String) = {
    val startTime = System.currentTimeMillis()
    val title = FileUtil.getFileName(path)
    val collection = getCollection(path)
    bookRepository.findByCollectionAndTitle(collection, title).toScala match {
      case Some(book) =>
        bookRepository.delete(book)
      case None =>
        // probably book was just moved inside the library
        log.debug(s"no book to delete for path $path")
    }
    val endTime = System.currentTimeMillis()
    val durationMilliseconds = endTime - startTime
    scanTimeMilliseconds.set(scanTimeMilliseconds.get() + durationMilliseconds)
    computeAndPrintStatistics()
  }

  private def adaptProgressToBook(oldProgress: Seq[Progress], newBook: Book) = {
    oldProgress.map(p => {
      val np = ProgressUtil.fixProgressForBook(p, newBook)
      np.id = p.id
      np
    })
  }

  private def verifyAndAddBook(path: String) = {
    val startTime = System.currentTimeMillis()
    val title = FileUtil.getFileName(path)
    val collection = getCollection(path)
    bookRepository.findByCollectionAndTitle(collection, title).toScala match {
      case Some(book) =>
        verifyExistingBook(path, book)
      case None =>
        // this may be a book that was moved
        val checksum = FileUtil.getFileChecksum(path)
        bookRepository.findById(checksum).toScala match {
          case Some(book) => refreshExistingBook(path, title, collection, book)
          case None => scanNewBook(path)
        }
    }
    val endTime = System.currentTimeMillis()
    val durationMilliseconds = endTime - startTime
    scanTimeMilliseconds.set(scanTimeMilliseconds.get() + durationMilliseconds)
    computeAndPrintStatistics()
  }

  private def scanNewBook(path: String) = {
    scanFile(path) match {
      case Some(book) =>
        scannedFiles.incrementAndGet()
        bookRepository.save(book)
        // look for orphaned progress with this path
        val oldProgress = progressRepository.findByTitleAndCollection(book.title, book.collection).asScala.toSeq
        val newProgress = adaptProgressToBook(oldProgress, book)
        if (newProgress.nonEmpty) progressRepository.saveAll(newProgress.asJava)
      case None =>
        scanFailures.incrementAndGet()
        log.debug(s"failed to scan book at path $path")
    }
  }

  private def refreshExistingBook(path: String, title: String, collection: String, book: Book) = {
    scannedFiles.incrementAndGet()
    book.collection = collection
    book.title = title
    book.mediaType = FileUtil.getExtensionWithCorrectCase(path)
    bookRepository.save(book)
  }

  private def verifyExistingBook(path: String, book: Book) = {
    val checksum = FileUtil.getFileChecksum(path)
    if (checksum != book.id) {
      val oldProgress = progressRepository.findByBookId(book.id).asScala.toSeq
      bookRepository.delete(book)
      // todo: but should probably have an admin method to delete orphaned progress
      scanFile(path) match {
        case Some(newBook) =>
          scannedFiles.incrementAndGet()
          bookRepository.save(newBook)
          val newProgress = adaptProgressToBook(oldProgress, newBook)
          if (newProgress.nonEmpty) progressRepository.saveAll(newProgress.asJava)
        case None =>
          scanFailures.incrementAndGet()
          log.debug(s"failed to scan book at $path")
      }
    } else {
      scannedFiles.incrementAndGet()
      log.debug(s"checksum checks out for $path")
    }
  }

  private def computeAndPrintStatistics() = {
    val processedFiles = scannedFiles.get() + scanFailures.get()
    val meanScanTime = (scanTimeMilliseconds.get().toDouble / processedFiles).toLong
    val remainingFiles = changesQueue.size()
    val remainingTime = remainingFiles * meanScanTime
    log.info(s"scanned $processedFiles" +
      s" (${scannedFiles.get()} successful, ${scanFailures.get()} failed)" +
      s" in ${DateUtil.millisToHumanReadable(scanTimeMilliseconds.get())}" +
      s" (mean scan time ${DateUtil.millisToHumanReadable(meanScanTime)});" +
      s" $remainingFiles files remaining, to be done in approximately ${DateUtil.millisToHumanReadable(remainingTime)}")
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
      val checksum = FileUtil.getFileChecksum(path)
      val title = FileUtil.getFileName(path)
      val collection = getCollection(path)
      val cover = CbrUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
      val size = CbrUtil.countPages(path)
      (cover, size) match {
        case (Some(c), Some(s)) =>
          val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
          Some(new Book(checksum, FileUtil.getExtensionWithCorrectCase(path), title, collection, c.mediaType, smallerCover, s, getFileCreationDate(path)))
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
      val checksum = FileUtil.getFileChecksum(path)
      val title = FileUtil.getFileName(path)
      val collection = getCollection(path)
      val cover = PdfUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
      val size = PdfUtil.countPages(path)
      (cover, size) match {
        case (Some(c), Some(s)) =>
          val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
          Some(new Book(checksum, FileUtil.getExtensionWithCorrectCase(path), title, collection, c.mediaType, smallerCover, s, getFileCreationDate(path)))
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
      val checksum = FileUtil.getFileChecksum(path)
      val title = FileUtil.getFileName(path)
      val collection = getCollection(path)
      val cover = CbzUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
      val size = CbzUtil.countPages(path)
      (cover, size) match {
        case (Some(c), Some(s)) =>
          val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
          Some(new Book(checksum, FileUtil.getExtensionWithCorrectCase(path), title, collection, c.mediaType, smallerCover, s, getFileCreationDate(path)))
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
      val checksum = FileUtil.getFileChecksum(path)
      val title = if (getTitleFromMetadata) EpubUtil.getTitle(path).getOrElse(FileUtil.getFileName(path)) else FileUtil.getFileName(path)
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
          val book = new Book(checksum, FileUtil.getExtensionWithCorrectCase(path), title, collection, c.mediaType, smallerCover, size, getFileCreationDate(path))
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
