package com.cacoveanu.reader.service

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.{ZipEntry, ZipFile}

import com.cacoveanu.reader.entity.{ComicProgress, DbComic, DbUser}
import com.cacoveanu.reader.repository.{ComicProgressRepository, ComicRepository}
import com.cacoveanu.reader.util.FileUtil
import com.github.junrar.Archive
import javax.annotation.PostConstruct
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional
import com.cacoveanu.reader.util.SeqUtil.AugmentedSeq
import com.github.junrar.rarfile.FileHeader
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.data.domain.{PageRequest, Sort}
import org.springframework.data.domain.Sort.Direction
import org.springframework.scheduling.annotation.Scheduled

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class ComicPage(num: Int, mediaType: String, data: Array[Byte])

object ComicService {
  val log: Logger = LoggerFactory.getLogger(classOf[ComicService])
}

@Service
class ComicService {

  import ComicService.log

  private val COMIC_TYPE_CBR = "cbr"
  private val COMIC_TYPE_CBZ = "cbz"
  private val COMIC_FILE_REGEX = ".+\\.(" + COMIC_TYPE_CBR + "|" + COMIC_TYPE_CBZ + ")$"
  private val COVER_RESIZE_MINIMAL_SIDE = 500
  private val PAGE_SIZE = 20
  private val COMIC_PART_SIZE = 20
  private val DB_UPDATE_BATCH_SIZE = 40

  private val scanningCollection = new AtomicBoolean(false)

  @Value("${comics.location}")
  @BeanProperty
  var comicsLocation: String = _

  @BeanProperty
  @Autowired var imageService: ImageService = _

  @BeanProperty
  @Autowired
  var comicRepository: ComicRepository = _

  @BeanProperty @Autowired var comicProgressRepository: ComicProgressRepository = _

  private implicit val executionContext = ExecutionContext.global

  @PostConstruct
  def updateLibrary() = scan(false)

  @Scheduled(cron = "0 0 * * * *")
  def scheduledRescan() = scan(false)

  def scan(force: Boolean = false) = Future {
    if (scanningCollection.compareAndSet(false, true)) {
      scanLibrary(comicsLocation, forceUpdate = force)
      scanningCollection.set(false)
    } else {
      log.info("failed to " + (if (force) "force " else "") + "update the library, scan already in progress")
    }
  }

  private def prepareSearchTerm(original: String): String = {
    val lowercase = original.toLowerCase()
    val pattern = "[A-Za-z0-9]+".r
    val matches: Regex.MatchIterator = pattern.findAllIn(lowercase)
    val result = "%" + matches.mkString("%") + "%"
    result
  }

  def searchComics(term: String, page: Int): Seq[DbComic] = {
    val sort = Sort.by(Direction.ASC, "collection", "title")
    val pageRequest = PageRequest.of(page, PAGE_SIZE, sort)
    comicRepository.search(prepareSearchTerm(term), pageRequest).asScala.toSeq
  }

  def getCollectionPage(page: Int): Seq[DbComic] = {
    val sort = Sort.by(Direction.ASC, "collection", "title")
    val pageRequest = PageRequest.of(page, PAGE_SIZE, sort)
    comicRepository.findAll(pageRequest).asScala.toSeq
  }

  def loadComicProgress(user: DbUser, comic: DbComic): Option[ComicProgress] = {
    val progress = comicProgressRepository.findByUserAndComic(user, comic)
    Option(progress)
  }

  def loadCollections(): Seq[String] = {
    comicRepository.findAllCollections().asScala.toSeq
  }

  def loadComicProgress(user: DbUser, comicId: Long): Option[ComicProgress] = {
    comicProgressRepository.findByUserAndComicId(user, comicId).asScala
  }

  def loadTopComicProgress(user: DbUser, limit: Int): Seq[ComicProgress] = {
    val sort = Sort.by(Direction.DESC, "last_update")
    val pageRequest = PageRequest.of(0, limit, sort)
    comicProgressRepository.findUnreadByUser(user, pageRequest).asScala.toSeq
  }

  def loadComicProgress(user: DbUser, comics: Seq[DbComic]): Seq[ComicProgress] = {
    comicProgressRepository.findByUserAndComicIn(user, comics.asJava).asScala.toSeq
  }

  def deleteComicProgress(progress: ComicProgress) = {
    comicProgressRepository.delete(progress)
  }

  def saveComicProgress(progress: ComicProgress) = {
    val existingProgress = comicProgressRepository.findByUserAndComic(progress.user, progress.comic)
    if (existingProgress != null) progress.id = existingProgress.id
    comicProgressRepository.save(progress)
  }

  def readCover(path: String): Option[ComicPage] = readPagesFromDisk(path, Some(Seq(0))).flatMap(pages => pages.headOption)

  def computePartNumberForPage(page: Int) = {
    page / COMIC_PART_SIZE
  }

  private def computePagesForPart(part: Int) = {
    (part * COMIC_PART_SIZE) until (part * COMIC_PART_SIZE + COMIC_PART_SIZE)
  }

  def loadComic(id: Long): Option[DbComic] = {
    comicRepository.findById(id).asScala
  }

  @Cacheable(Array("parts"))
  def loadComicPart(id: Long, part: Int): Option[DbComic] = {
    comicRepository.findById(id).asScala match {
      case Some(dbComic) => readPagesFromDisk(dbComic.path, Some(computePagesForPart(part))) match {
        case Some(pages) =>
          dbComic.pages = pages
          Some(dbComic)
        case None => None
      }
      case None => None
    }
  }

  private def readPagesFromDisk(path: String, pages: Option[Seq[Int]] = None): Option[Seq[ComicPage]] = {
    FileUtil.getExtension(path) match {
      case COMIC_TYPE_CBR => readCbrPagesFromDisk(path, pages)
      case COMIC_TYPE_CBZ => readCbzPagesFromDisk(path, pages)
      case _ => None
    }
  }

  private[service] def countPagesFromDisk(path: String): Option[Int] =
    FileUtil.getExtension(path) match {
      case COMIC_TYPE_CBR => countCbrPagesFromDisk(path)
      case COMIC_TYPE_CBZ => countCbzPagesFromDisk(path)
      case _ => None
    }

  private def getValidCbrPages(archive: Archive) = {
    archive.getFileHeaders.asScala.toSeq
      .filter(f => !f.isDirectory)
      .filter(f => imageService.isImageType(f.getFileNameString))
      .sortBy(f => f.getFileNameString)
  }

  private def countCbrPagesFromDisk(path: String): Option[Int] = {
    var archive: Archive = null
    try {
      archive = new Archive(new FileInputStream(path))
      Some(getValidCbrPages(archive).size)
    } catch {
      case e: Throwable =>
        log.error("failed to read comic pages for " + path, e)
        None
    } finally {
      archive.close()
    }
  }

  private def readCbrPagesFromDisk(path: String, pages: Option[Seq[Int]] = None): Option[Seq[ComicPage]] = {
    var archive: Archive = null

    try {
      archive = new Archive(new FileInputStream(path))

      val sortedImageFiles: Seq[(FileHeader, Int)] = getValidCbrPages(archive).zipWithIndex

      val selectedImageFiles: Seq[(FileHeader, Int)] = pages match {
        case Some(pgs) =>
          sortedImageFiles.filter { case (_, index) => pgs.contains(index) }
        case None =>
          sortedImageFiles
      }

      val selectedImageData: Seq[ComicPage] = selectedImageFiles
        .flatMap{ case(archiveFile, index) => imageService.getMediaType(archiveFile.getFileNameString) match {
          case Some(mediaType) =>
            val fileContents = new ByteArrayOutputStream()
            archive.extractFile(archiveFile, fileContents)
            Some(ComicPage(index, mediaType, fileContents.toByteArray))
          case None => None
        }}

      Some(selectedImageData)
    } catch {
      case e: Throwable =>
        log.error("failed to read comic pages for " + path, e)
        None
    } finally {
      archive.close()
    }
  }

  private def getValidCbzPages(zipFile: ZipFile) = {
    zipFile.entries().asScala
      .filter(f => !f.isDirectory)
      .filter(f => imageService.isImageType(f.getName))
      .toSeq
      .sortBy(f => f.getName)
  }

  private def countCbzPagesFromDisk(path: String): Option[Int] = {
    var zipFile: ZipFile = null
    try {
      zipFile = new ZipFile(path)
      Some(getValidCbzPages(zipFile).size)
    } catch {
      case e: Throwable =>
        log.error("failed to read comic pages for " + path, e)
        None
    } finally {
      zipFile.close()
    }
  }

  private def readCbzPagesFromDisk(path: String, pages: Option[Seq[Int]] = None): Option[Seq[ComicPage]] = {
    var zipFile: ZipFile = null

    try {
      zipFile = new ZipFile(path)

      val sortedImageFiles: Seq[(ZipEntry, Int)] = getValidCbzPages(zipFile).zipWithIndex

      val selectedImageFiles: Seq[(ZipEntry, Int)] = pages match {
        case Some(pgs) =>
          sortedImageFiles.filter { case (_, index) => pgs.contains(index) }
        case None =>
          sortedImageFiles
      }

      val selectedImageData: Seq[ComicPage] = selectedImageFiles
        .flatMap{ case (file, index) => imageService.getMediaType(file.getName) match {
          case Some(mediaType) =>
            val fileContents = zipFile.getInputStream(file)
            val bos = new ByteArrayOutputStream()
            IOUtils.copy(fileContents, bos)
            Some(ComicPage(index, mediaType, bos.toByteArray))
          case None => None
        }}

      Some(selectedImageData)
    } catch {
      case e: Throwable =>
        log.error("failed to read comic pages for " + path, e)
        None
    } finally {
      zipFile.close()
    }
  }

  private[service] def getComicTitle(path: String): Option[String] = {
    Option(FileUtil.getFileName(path))
  }

  private[service] def getComicCollection(path: String): Option[String] = {
    val pathObject = Paths.get(path);
    val collectionPath = Paths.get(comicsLocation).relativize(pathObject.getParent)
    Some(collectionPath.toString)
  }

  def loadComicFromDatabase(id: Long): Option[DbComic] = {
    comicRepository.findById(id).asScala
  }

  def loadComicMetadataFromDisk(file: String): Option[DbComic] =
    (getComicTitle(file), getComicCollection(file), readCover(file), countPagesFromDisk(file)) match {
      case (Some(title), Some(collection), Some(cover), Some(totalPages)) =>
        imageService.getFormatName(cover.mediaType) match {
          case Some(formatName) =>
            val smallerCoverData = imageService.resizeImageByMinimalSide(cover.data, formatName, COVER_RESIZE_MINIMAL_SIDE)
            Some(new DbComic(file, title, collection, cover.mediaType, smallerCoverData, totalPages))
          case None => None
        }
      case _ =>
        log.info(s" failed to load metadata for $file")
        None
    }

  private def scanLibrary(libraryPath: String, forceUpdate: Boolean = false): Unit = {
    log.info(s"scanning (forced=$forceUpdate) library at $libraryPath")
    val comicsInDatabase = comicRepository.findAll().asScala
    val comicPathsInDatabase = comicsInDatabase.map(c => c.path)
    val filesInLibrary = FileUtil.scanFilesRegex(libraryPath, COMIC_FILE_REGEX)
    val newFiles = filesInLibrary.filter(f => ! comicPathsInDatabase.contains(f))
    comicsInDatabase.filter(c => !filesInLibrary.contains(c.path))
      .toSeq
      .toBatches(DB_UPDATE_BATCH_SIZE)
      .foreach(batch => {
        comicRepository.deleteAll(batch.asJava)
      })
    newFiles
      .toBatches(DB_UPDATE_BATCH_SIZE)
      .foreach(batch => {
        val toSave = batch.flatMap(f => loadComicMetadataFromDisk(f))
        comicRepository.saveAll(toSave.asJava)
      })

    if (forceUpdate) {
      comicsInDatabase
        .filter(c => comicPathsInDatabase.contains(c.path))
        .toSeq
        .toBatches(DB_UPDATE_BATCH_SIZE)
        .foreach(batch => {
          val toSave = batch.flatMap(c => loadComicMetadataFromDisk(c.path) match {
            case Some(updatedComic) =>
              updatedComic.id = c.id
              Some(updatedComic)
            case None => None
          })
          comicRepository.saveAll(toSave.asJava)
        })
    }
    log.info(s"finished scanning (forced=$forceUpdate) library at $libraryPath")
  }
}