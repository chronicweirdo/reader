package com.cacoveanu.reader.service

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.nio.file.Paths
import java.util.concurrent.{ExecutorService, Executors}
import java.util.zip.ZipFile

import com.cacoveanu.reader.repository.{ComicProgressRepository, ComicRepository}
import com.cacoveanu.reader.util.FileUtil
import com.github.junrar.Archive
import javax.annotation.PostConstruct
import javax.persistence.{CascadeType, Column, Entity, FetchType, GeneratedValue, GenerationType, Id, JoinColumn, ManyToOne, OneToMany, OneToOne, Table, UniqueConstraint}
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

@Entity
class DbComic {
  @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: java.lang.Long = _
  @Column(unique = true) var path: String = _
  var title: String = _
  var collection: String = _
  var mediaType: MediaType = _
  var cover: Array[Byte] = _

  def this(path: String, title: String, collection: String, mediaType: MediaType, cover: Array[Byte]) {
    this()
    this.path = path
    this.title = title
    this.collection = collection
    this.mediaType = mediaType
    this.cover = cover
  }
}

@Entity
@Table(uniqueConstraints=Array(new UniqueConstraint(columnNames = Array("userId", "comicId"))))
/*(uniqueConstraints=Array(
  @UniqueConstraint(columnNames = Array("productId", "serial"))
))*/
class ComicProgress {
  @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: java.lang.Long = _
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="userId")
  var user: DbUser = _
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="comicId")
  var comic: DbComic = _
  var page: Int = _
}

case class ComicPage(mediaType: MediaType, data: Array[Byte])

case class FullComic(path: String, title: String, collection: String, pages: Seq[ComicPage])

@Service
class ComicService {

  private val COMIC_TYPE_CBR = "cbr"
  private val COMIC_TYPE_CBZ = "cbz"
  private val COMIC_FILE_REGEX = ".+\\.(" + COMIC_TYPE_CBR + "|" + COMIC_TYPE_CBZ + ")$"
  private val COVER_RESIZE_MINIMAL_SIDE = 300

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
  def updateLibrary() = Future {
    scanLibrary(comicsLocation)
  }

  def forceUpdateLibrary() = Future {
    scanLibrary(comicsLocation, forceUpdate = true)
  }

  def getCollection(): Seq[DbComic] = {
    comicRepository.findAll().asScala.toSeq
  }

  def loadComicProgress(user: DbUser, comic: DbComic): Option[ComicProgress] = {
    val progress = comicProgressRepository.findByUserAndComic(user, comic)
    if (progress != null) Some(progress)
    else None
  }

  def saveComicProgress(progress: ComicProgress) = {
    val existingProgress = comicProgressRepository.findByUserAndComic(progress.user, progress.comic)
    if (existingProgress != null) progress.id = existingProgress.id
    comicProgressRepository.save(progress)
  }

  private def isImageType(fileName: String) =
    Seq("jpg", "jpeg", "png", "gif") contains FileUtil.getExtension(fileName)

  def readPage(path: String, pageNumber: Int): Option[ComicPage] = {
    FileUtil.getExtension(path) match {
      case COMIC_TYPE_CBR => readCbrPage(path, pageNumber)
      case COMIC_TYPE_CBZ => readCbzPage(path, pageNumber)
      case _ => None
    }
  }

  @Cacheable(Array("comics"))
  def loadFullComic(id: Long): Option[FullComic] = {
    comicRepository.findById(id).asScala match {
      case Some(dbComic) =>
        FileUtil.getExtension(dbComic.path) match {
          case COMIC_TYPE_CBR => loadCbr(dbComic.path)
          case COMIC_TYPE_CBZ => loadCbz(dbComic.path)
          case _ => None
        }
      case None => None
    }
  }

  private def loadCbr(path: String): Option[FullComic] = {
    var archive: Archive = null

    val pages = try {
      archive = new Archive(new FileInputStream(path))

      Some(
        archive.getFileHeaders.asScala
          .filter(f => !f.isDirectory)
          .filter(f => isImageType(f.getFileNameString))
          .sortBy(f => f.getFileNameString)
          .flatMap(archiveFile => imageService.getMediaType(archiveFile.getFileNameString) match {
            case Some(mediaType) =>
              val fileContents = new ByteArrayOutputStream()
              archive.extractFile(archiveFile, fileContents)
              Some(ComicPage(mediaType, fileContents.toByteArray))
            case None => None
          })
          .toSeq
      )
    } catch {
      case _: Throwable => None
    } finally {
      archive.close()
    }

    (pages, getComicTitle(path), getComicCollection(path)) match {
      case (Some(binaryPages), Some(title), Some(collection)) => Some(FullComic(path, title, collection, binaryPages))
      case _ => None
    }
  }

  private def readCbrPage(path: String, pageNumber: Int): Option[ComicPage] = {
    println("scanning comic: " + path)
    val archive = new Archive(new FileInputStream(path))
    val fileHeaders = archive.getFileHeaders().asScala
      .filter(f => !f.isDirectory)
      .filter(f => isImageType(f.getFileNameString))

    if (fileHeaders.indices contains pageNumber) {
      val sortedFileHandlers = fileHeaders.sortBy(f => f.getFileNameString)
      val archiveFile = sortedFileHandlers(pageNumber)
      val fileMediaType: Option[MediaType] = imageService.getMediaType(archiveFile.getFileNameString)
      val fileContents = new ByteArrayOutputStream()
      archive.extractFile(archiveFile, fileContents)
      archive.close()

      fileMediaType match {
        case Some(mediaType) => Some(ComicPage(mediaType, fileContents.toByteArray))
        case None => None
      }
    } else None
  }

  private def loadCbz(path: String): Option[FullComic] = {
    var zipFile: ZipFile = null

    val pages = try {
      zipFile = new ZipFile(path)

      Some(
        zipFile.entries().asScala
          .filter(f => !f.isDirectory)
          .filter(f => isImageType(f.getName))
          .toSeq
          .sortBy(f => f.getName)
          .flatMap(file => imageService.getMediaType(file.getName) match {
            case Some(mediaType) =>
              val fileContents = zipFile.getInputStream(file)
              val bos = new ByteArrayOutputStream()
              IOUtils.copy(fileContents, bos)
              Some(ComicPage(mediaType, bos.toByteArray))
            case None => None
          })
          .toSeq
      )
    } catch {
      case _: Throwable => None
    } finally {
      zipFile.close()
    }

    (pages, getComicTitle(path), getComicCollection(path)) match {
      case (Some(binaryPages), Some(title), Some(collection)) =>
        Some(FullComic(path, title, collection, binaryPages))
      case _ => None
    }

  }

  private def readCbzPage(path: String, pageNumber: Int): Option[ComicPage] = {
    val zipFile = new ZipFile(path)
    val files = zipFile.entries().asScala
      .filter(f => !f.isDirectory)
      .filter(f => isImageType(f.getName))
      .toSeq

    if (files.indices contains pageNumber) {
      val sortedFiles = files.sortBy(f => f.getName)
      val file = sortedFiles(pageNumber)
      val fileMediaType = imageService.getMediaType(file.getName)
      val fileContents = zipFile.getInputStream(file)
      val bos = new ByteArrayOutputStream()
      IOUtils.copy(fileContents, bos)
      zipFile.close();

      fileMediaType match {
        case Some(mediaType) => Some(ComicPage(mediaType, bos.toByteArray))
        case None => None
      }
    } else None
  }

  private def getComicTitle(path: String): Option[String] = {
    val pathObject = Paths.get(path);
    val fileName = pathObject.getFileName.toString
    Some(fileName.substring(0, fileName.lastIndexOf('.')))
  }

  private def getComicCollection(path: String): Option[String] = {
    val pathObject = Paths.get(path);
    val collectionPath = Paths.get(comicsLocation).relativize(pathObject.getParent)
    Some(collectionPath.toString)
  }

  def loadComicFromDatabase(id: Long): Option[DbComic] = {
    comicRepository.findById(id).asScala
  }

  def loadComic(file: String): Option[DbComic] =
    (getComicTitle(file), getComicCollection(file), readPage(file, 0)) match {
      case (Some(title), Some(collection), Some(cover)) =>
        imageService.getFormatName(cover.mediaType) match {
          case Some(formatName) =>
            val smallerCoverData = imageService.resizeImageByMinimalSide(cover.data, formatName, COVER_RESIZE_MINIMAL_SIDE)
            Some(new DbComic(file, title, collection, cover.mediaType, smallerCoverData))
          case None => None
        }
      case _ => None
    }

  private def scanLibrary(libraryPath: String, forceUpdate: Boolean = false): Unit = {
    val comicsInDatabase = comicRepository.findAll().asScala
    val comicPathsInDatabase = comicsInDatabase.map(c => c.path)
    val filesInLibrary = scanFilesRegex(libraryPath, COMIC_FILE_REGEX)
    val newFiles = filesInLibrary.filter(f => ! comicPathsInDatabase.contains(f))
    val comicsToDelete = comicsInDatabase.filter(c => !filesInLibrary.contains(c.path))
    comicRepository.deleteAll(comicsToDelete.asJava)
    val newComics = newFiles.flatMap(f => loadComic(f))
    comicRepository.saveAll(newComics.asJava)

    if (forceUpdate) {
      val updatedComics = comicsInDatabase
        .filter(c => comicPathsInDatabase.contains(c.path))
        .flatMap(c => loadComic(c.path) match {
            case Some(updatedComic) =>
              updatedComic.id = c.id
              Some(updatedComic)
            case None => None
        })
      comicRepository.saveAll(updatedComics.asJava)
    }
  }

  private def scan(path: String): Seq[File] = {
    var files = mutable.Seq[File]()
    files = files :+ new File(path)
    var processed = 0
    while (processed < files.length) {
      val current = files(processed)
      if (current.exists() && current.isDirectory()) {
        val children: Array[File] = current.listFiles()
        files ++= current.listFiles
      }
      processed += 1
    }

    files.toSeq
  }

  private def scanFilesRegex(path: String, regex: String) = {
    val pattern = regex.r
    scan(path)
      .filter(f => f.isFile)
      .filter(f => pattern.pattern.matcher(f.getAbsolutePath).matches)
      .map(f => f.getAbsolutePath)
  }
}