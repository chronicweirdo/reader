package com.cacoveanu.reader.service

import java.nio.file.{FileSystems, Files, Path, Paths, WatchKey}
import com.cacoveanu.reader.entity.{Book, Content, Progress}
import com.cacoveanu.reader.repository.{BookRepository, ProgressRepository}
import com.cacoveanu.reader.service.BookFolderChangeType.{ADDED, BookFolderChangeType, DELETED, MODIFIED}
import com.cacoveanu.reader.util.{CbrUtil, CbzUtil, EpubUtil, FileMediaTypes, FileTypes, FileUtil, PdfUtil, ProgressUtil}

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
import java.util.Date
import java.util.concurrent.{BlockingQueue, ConcurrentHashMap, LinkedBlockingQueue}
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.OptionConverters._


object BookFolderChangeType extends Enumeration {
  type BookFolderChangeType = Value
  val ADDED, MODIFIED, DELETED = Value
}

case class BookFolderChange(path: String, isFile: Boolean, typ: BookFolderChangeType)

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
  def init() = {
    initialScanFolder(libraryLocation)
    startWatcher()
    startQueueConsumer()
  }

  //@Scheduled(cron = "0 */30 * * * *")
  //def scheduledRescan() = scan()

  private implicit val executionContext = ExecutionContext.global

  //private var lastScanDate: Date = _

  //private val scanInProgress: AtomicBoolean = new AtomicBoolean(false)

  val watchService = FileSystems.getDefault().newWatchService()
  val watchServiceKeyMap = new ConcurrentHashMap[String, WatchKey]()
  val changesQueue: BlockingQueue[BookFolderChange] = new LinkedBlockingQueue[BookFolderChange]()
  //private val handlingEvents: AtomicBoolean = new AtomicBoolean(false)

  def startWatching(path: String): Unit = {
    val key: WatchKey = Paths.get(path).register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    watchServiceKeyMap.put(path, key)
  }

  def stopWatching(path: String) = {
    watchServiceKeyMap.keys().asScala.filter(k => k.startsWith(path)).foreach(k => {
      val key = watchServiceKeyMap.get(k)
      key.cancel()
      watchServiceKeyMap.remove(k)
    })
  }

  /*private def setLastScanDate() = synchronized {
    lastScanDate = new Date()
  }

  def getLastScanDate(): Date = {
    if (lastScanDate != null) lastScanDate.clone().asInstanceOf[Date]
    else null
  }*/

  def initialScanFolder(path: String) = {
    //FileUtil.scanFolderTree(path).foreach(f => changesQueue.put(BookFolderChange(f, false, ADDED)))
    FileUtil.scanFolderTree(path).foreach(f => startWatching(f))

    val filesOnDisk = FileUtil.scanFilesRegex(path, SUPPORTED_FILES_REGEX).toSet
    val filesInDatabase = bookRepository.findAllPaths().asScala.filter(p => p.startsWith(path)).toSet

    val newFiles = filesOnDisk.diff(filesInDatabase)
    log.info(s"found ${newFiles.size} new files in initial scan")
    val deletedFiles = filesInDatabase.diff(filesOnDisk)
    log.info(s"found ${deletedFiles.size} deleted files in initial scan")
    val modifiedFiles = filesInDatabase.diff(deletedFiles)
    log.info(s"found ${modifiedFiles.size} modified files in initial scan")

    deletedFiles.foreach(f => changesQueue.put(BookFolderChange(f, true, DELETED)))
    newFiles.foreach(f => changesQueue.put(BookFolderChange(f, true, ADDED)))
    modifiedFiles.foreach(f => changesQueue.put(BookFolderChange(f, true, MODIFIED)))
  }

  def startWatcher() = {
    new Thread(() => {
      var key: WatchKey = null
      while ( {
        key = watchService.take
        key != null
      }) {
        for (event <- key.pollEvents.asScala) {
          //var eventPath = key.watchable().asInstanceOf[Path].resolveSibling(event.context())
          val eventFile = key.watchable().asInstanceOf[Path].resolve(event.context().asInstanceOf[Path]).toFile
          val eventPath = eventFile.getAbsolutePath
          //System.out.println("Event kind:" + event.kind + ". File affected: " + eventPath)
          val typ = event.kind() match {
            case ENTRY_CREATE => ADDED
            case ENTRY_MODIFY => MODIFIED
            case ENTRY_DELETE => DELETED
          }
          changesQueue.put(BookFolderChange(eventPath, eventFile.isFile, typ))
        }
        key.reset
      }
    }).start()
  }

  def startQueueConsumer() = {
    new Thread(() => {
      while (true) {
        val change = changesQueue.take()
        //println(change)
        change match {
          case BookFolderChange(path, _, DELETED) =>
            log.info(s"stop following changes in possible folder $path and delete possible book at $path")
            stopWatching(path)
            deleteBook(path)
          case BookFolderChange(path, false, ADDED) =>
            log.info(s"start following changes in folder $path")
            //startWatching(path)
            initialScanFolder(path)
          case BookFolderChange(path, true, ADDED) =>
            log.info(s"scan new book $path")
            verifyAndAddBook(path)
          //case BookFolderChange(path, false, DELETED) =>
          //  println(s"delete book $path")
          case BookFolderChange(path, true, MODIFIED) =>
            log.info(s"rescan and update book $path")
            verifyAndAddBook(path)
          case _ =>
            log.info("do nothing")
        }
      }
    }).start()
  }

  def deleteBook(path: String) = {
    bookRepository.findByPath(path).toScala match {
      case Some(book) => bookRepository.delete(book)
      case None => log.info(s"no book to delete for path $path")
    }
  }

  def adaptProgressToBook(oldProgress: Seq[Progress], newBook: Book) = {
    oldProgress.map(p => {
      val np = ProgressUtil.fixProgressForBook(p, newBook)
      np.id = p.id
      np
    })
  }

  def verifyAndAddBook(path: String) = {
    bookRepository.findByPath(path).toScala match {
      case Some(book) =>
        val checksum = FileUtil.getFileChecksum(path)
        if (checksum != book.id) {
          // find progress for old version of book
          val oldProgress = progressRepository.findByBookId(book.id).asScala.toSeq
          // delete old version of book
          bookRepository.delete(book)
          // todo: but should probably have an admin method to delete orphaned progress
          // todo: or just show orphaned progress in the history page, allow users to delete (or match to existing book?)
          // rescan book
          scanFile(path) match {
            case Some(newBook) =>
              bookRepository.save(newBook)
              val newProgress = adaptProgressToBook(oldProgress, newBook)
              if (newProgress.nonEmpty) progressRepository.saveAll(newProgress.asJava)
            case None =>
              log.info(s"failed to scan book at $path")
          }
        }
      case None =>
        // this is a completely new book
        scanFile(path) match {
          case Some(book) =>
            bookRepository.save(book)
            // look for orphaned progress with this path
            val oldProgress = progressRepository.findByTitleAndCollection(book.title, book.collection).asScala.toSeq
            val newProgress = adaptProgressToBook(oldProgress, book)
            if (newProgress.nonEmpty) progressRepository.saveAll(newProgress.asJava)
          case None => log.info(s"failed scanning book at path $path")
        }
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
      val checksum = FileUtil.getFileChecksum(path)
      val title = FileUtil.getFileName(path)
      val collection = getCollection(path)
      val cover = CbrUtil.readPages(path, Some(Seq(0))).flatMap(pages => pages.headOption)
      val size = CbrUtil.countPages(path)
      (cover, size) match {
        case (Some(c), Some(s)) =>
          val smallerCover = imageService.resizeImageByMinimalSide(c.data, c.mediaType, COVER_RESIZE_MINIMAL_SIDE)
          Some(new Book(checksum, path, title, collection, c.mediaType, smallerCover, s, getFileCreationDate(path)))
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
          Some(new Book(checksum, path, title, collection, c.mediaType, smallerCover, s, getFileCreationDate(path)))
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
          Some(new Book(checksum, path, title, collection, c.mediaType, smallerCover, s, getFileCreationDate(path)))
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
          val book = new Book(checksum, path, title, collection, c.mediaType, smallerCover, size, getFileCreationDate(path))
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
