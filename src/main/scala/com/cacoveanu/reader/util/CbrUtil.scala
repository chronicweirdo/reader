package com.cacoveanu.reader.util

import java.io.{ByteArrayOutputStream, FileInputStream}

import com.cacoveanu.reader.service.{ComicPage, ComicService}
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._

object CbrUtil {

  private val log: Logger = LoggerFactory.getLogger(CbrUtil.getClass)

  private def getValidCbrPages(archive: Archive) = {
    archive.getFileHeaders.asScala.toSeq
      .filter(f => !f.isDirectory)
      .filter(f => FileUtil.isImageType(f.getFileNameString))
      .sortBy(f => f.getFileNameString)
  }

  def countPages(path: String): Option[Int] = {
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

  def readPages(path: String, pages: Option[Seq[Int]] = None): Option[Seq[ComicPage]] = {
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
        .flatMap{ case(archiveFile, index) => FileUtil.getMediaType(archiveFile.getFileNameString) match {
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
}
