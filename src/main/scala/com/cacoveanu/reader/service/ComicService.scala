package com.cacoveanu.reader.service

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.nio.file.Paths
import java.util.concurrent.{ExecutorService, Executors}
import java.util.zip.ZipFile

import com.cacoveanu.reader.repository.ComicRepository
import com.cacoveanu.reader.util.FileUtil
import com.github.junrar.Archive
import javax.annotation.PostConstruct
import javax.persistence.{Column, Entity, GeneratedValue, GenerationType, Id, UniqueConstraint}
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
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  var id: Int = _
  @Column(unique=true)
  var path: String = _
  var title: String = _
  var mediaType: MediaType = _
  var cover: Array[Byte] = _
}

case class Comic(title: String, path: String, cover: ComicPage)

case class ComicPage(mediaType: MediaType, data: Array[Byte])

case class FullComic(comic: Comic, pages: Seq[ComicPage])

@Service
class ComicService {

  private val COMIC_TYPE_CBR = "cbr"
  private val COMIC_TYPE_CBZ = "cbz"
  private val COMIC_FILE_REGEX = ".+\\.(" + COMIC_TYPE_CBR + "|" + COMIC_TYPE_CBZ + ")$"
  //private val COVER_RESIZE_FACTOR = .2
  private val COVER_RESIZE_MINIMAL_SIDE = 500 //300

  @Value("${comics.location}")
  @BeanProperty
  var comicsLocation: String = null

  @BeanProperty
  @Autowired var imageService: ImageService = null

  @BeanProperty
  @Autowired
  var comicRepository: ComicRepository = null

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
  def loadFullComic(id: Int): Option[FullComic] = {
    comicRepository.findById(id).asScala match {
      case Some(dbComic) =>
        FileUtil.getExtension(dbComic.path) match {
          case COMIC_TYPE_CBR => Some(loadCbr(dbComic.path))
          case COMIC_TYPE_CBZ => Some(loadCbz(dbComic.path))
          case _ => None
        }
      case None => None
    }
  }

  private def loadCbr(path: String): FullComic = {
    val archive = new Archive(new FileInputStream(path))
    val fileHeaders = archive.getFileHeaders().asScala
      .filter(f => !f.isDirectory)
      .filter(f => isImageType(f.getFileNameString))

    val sortedFileHandlers = fileHeaders.sortBy(f => f.getFileNameString)
    val pages = sortedFileHandlers.map(archiveFile => {
      val fileMediaType: Option[MediaType] = imageService.getMediaType(archiveFile.getFileNameString)
      val fileContents = new ByteArrayOutputStream()
      archive.extractFile(archiveFile, fileContents)

      fileMediaType match {
        case Some(mediaType) => Some(ComicPage(mediaType, fileContents.toByteArray))
        case None => None
      }
    }).filter(pageOption => pageOption.isDefined)
      .map(pageOption => pageOption.get)
      .toSeq

    archive.close()

    FullComic(Comic(getComicTitle(path).get, path, pages.head), pages)
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

  private def loadCbz(path: String): FullComic = {
    val zipFile = new ZipFile(path)
    val files = zipFile.entries().asScala
      .filter(f => !f.isDirectory)
      .filter(f => isImageType(f.getName))
      .toSeq

    val sortedFiles = files.sortBy(f => f.getName)

    val pages = sortedFiles.map(file => {
      val fileMediaType = imageService.getMediaType(file.getName)
      val fileContents = zipFile.getInputStream(file)
      val bos = new ByteArrayOutputStream()
      IOUtils.copy(fileContents, bos)

      fileMediaType match {
        case Some(mediaType) => Some(ComicPage(mediaType, bos.toByteArray))
        case None => None
      }
    }).filter(pageOption => pageOption.isDefined)
      .map(pageOption => pageOption.get)
      .toSeq
    zipFile.close();

    FullComic(Comic(getComicTitle(path).get, path, pages.head), pages)
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

  def loadComic(file: String): Option[Comic] =
    (getComicTitle(file), readPage(file, 0)) match {
      case (Some(title), Some(cover)) =>
        imageService.getFormatName(cover.mediaType) match {
          case Some(formatName) =>
            val smallerCoverData = imageService.resizeImageByMinimalSide(cover.data, formatName, COVER_RESIZE_MINIMAL_SIDE)
            val smallerCover = cover.copy(data = smallerCoverData)
            Some(Comic(title, file, smallerCover))
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
    val newComics = newFiles.map(f => loadComic(f))
      .filter(comicOptional => comicOptional.isDefined)
      .map(comicOptional => comicOptional.get)
      .map(c => {
        val dc = new DbComic
        dc.path = c.path
        dc.title = c.title
        dc.mediaType = c.cover.mediaType
        dc.cover = c.cover.data
        dc
      })
    comicRepository.saveAll(newComics.asJava)

    if (forceUpdate) {
      val updatedComics = comicsInDatabase.filter(c => comicPathsInDatabase.contains(c.path))
        .map(c => {
          loadComic(c.path) match {
            case Some(updatedComic) =>
              c.title = updatedComic.title
              c.mediaType = updatedComic.cover.mediaType
              c.cover = updatedComic.cover.data
              Some(c)
            case None => None
          }
        }).filter(o => o.isDefined).map(o => o.get)
      comicRepository.saveAll(updatedComics.asJava)
    }
  }

  def loadComicFiles(path: String): Seq[Comic] =
    scanFilesRegex(path, COMIC_FILE_REGEX)
      .map(file => loadComic(file))
      .filter(comic => comic.isDefined)
      .map(comic => comic.get)

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
      .filter(f => f.isFile())
      .filter(f => pattern.pattern.matcher(f.getAbsolutePath).matches)
      .map(f => f.getAbsolutePath())
  }
}