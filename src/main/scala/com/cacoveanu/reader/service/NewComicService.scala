package com.cacoveanu.reader.service

import java.io.{ByteArrayOutputStream, FileInputStream}
import java.nio.file.Paths
import java.util
import java.util.zip.ZipFile

import com.cacoveanu.reader.util.FolderUtil
import com.github.junrar.Archive
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

import scala.collection.JavaConverters._
import scala.collection.mutable

case class NewComic(title: String, path: String, cover: NewComicPage)

case class NewComicPage(mediaType: MediaType, data: Array[Byte])

@Service
class NewComicService {

  private val COMIC_TYPE_CBR = "cbr"
  private val COMIC_TYPE_CBZ = "cbz"
  private val COMIC_FILE_REGEX = ".+\\.(" + COMIC_TYPE_CBR + "|" + COMIC_TYPE_CBZ + ")$"

  private def getExtension(path: String) = path.toLowerCase().substring(path.lastIndexOf('.')).substring(1)

  private def getMediaType(fileName: String): Option[MediaType] =
    getExtension(fileName) match {
      case "jpg" => Some(MediaType.IMAGE_JPEG)
      case "jpeg" => Some(MediaType.IMAGE_JPEG)
      case "png" => Some(MediaType.IMAGE_PNG)
      case "gif" => Some(MediaType.IMAGE_GIF)
      case _ => None
    }

  private def isImageType(fileName: String) =
    Seq("jpg", "jpeg", "png", "gif") contains getExtension(fileName)

  def readPage(path: String, pageNumber: Int): Option[NewComicPage] = {
    getExtension(path) match {
      case COMIC_TYPE_CBR => readCbrPage(path, pageNumber)
      case COMIC_TYPE_CBZ => readCbzPage(path, pageNumber)
      case _ => None
    }
  }

  private def readCbrPage(path: String, pageNumber: Int): Option[NewComicPage] = {

    val archive = new Archive(new FileInputStream(path))
    val fileHeaders = archive.getFileHeaders().asScala
      .filter(f => !f.isDirectory)
      .filter(f => isImageType(f.getFileNameString))

    if (fileHeaders.indices contains pageNumber) {
      val sortedFileHandlers = fileHeaders.sortBy(f => f.getFileNameString)
      val archiveFile = sortedFileHandlers(pageNumber)
      val fileMediaType: Option[MediaType] = getMediaType(archiveFile.getFileNameString)
      val fileContents = new ByteArrayOutputStream()
      archive.extractFile(archiveFile, fileContents)
      archive.close()

      fileMediaType match {
        case Some(mediaType) => Some(NewComicPage(mediaType, fileContents.toByteArray))
        case None => None
      }
    } else None
  }

  private def readCbzPage(path: String, pageNumber: Int): Option[NewComicPage] = {
    val zipFile = new ZipFile(path)
    val files = zipFile.entries().asScala
      .filter(f => !f.isDirectory)
      .filter(f => isImageType(f.getName))
      .toSeq

    if (files.indices contains pageNumber) {
      val sortedFiles = files.sortBy(f => f.getName)
      val file = sortedFiles(pageNumber)
      val fileMediaType = getMediaType(file.getName)
      val fileContents = zipFile.getInputStream(file)
      val bos = new ByteArrayOutputStream()
      IOUtils.copy(fileContents, bos)
      zipFile.close();

      fileMediaType match {
        case Some(mediaType) => Some(NewComicPage(mediaType, bos.toByteArray))
        case None => None
      }
    } else None
  }

  private def getComicTitle(path: String): Option[String] = {
    val pathObject = Paths.get(path);
    val fileName = pathObject.getFileName.toString
    Some(fileName.substring(0, fileName.lastIndexOf('.')))
  }

  private def loadComic(file: String): Option[NewComic] =
    (getComicTitle(file), readPage(file, 0)) match {
      case (Some(title), Some(cover)) => Some(NewComic(title, file, cover))
      case _ => None
    }

  def loadComicFiles(path: String): mutable.Seq[NewComic] =
    FolderUtil.scanFilesRegex(path, COMIC_FILE_REGEX).asScala
    .map(file => loadComic(file))
    .filter(comic => comic.isDefined)
    .map(comic => comic.get)
}