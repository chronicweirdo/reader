package com.cacoveanu.reader.util

import java.io.ByteArrayOutputStream
import java.util.zip.{ZipEntry, ZipFile}

import com.cacoveanu.reader.entity.Content
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._

object CbzUtil {

  private val log: Logger = LoggerFactory.getLogger(CbzUtil.getClass)

  private def getValidCbzPages(zipFile: ZipFile) = {
    zipFile.entries().asScala
      .filter(f => !f.isDirectory)
      .filter(f => FileUtil.isImageType(f.getName))
      .toSeq
      .sortBy(f => f.getName)
  }

  def countPages(path: String): Option[Int] = {
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

  private def contentToByteArray(zipFile: ZipFile, file: ZipEntry) = {
    val fileContents = zipFile.getInputStream(file)
    val bos = new ByteArrayOutputStream()
    IOUtils.copy(fileContents, bos)
    bos.toByteArray
  }

  def readPages(path: String, pages: Option[Seq[Int]] = None): Option[Seq[Content]] = {
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

      val selectedImageData: Seq[Content] = selectedImageFiles
        .flatMap{ case (file, index) => FileUtil.getMediaType(file.getName) match {
          case Some(mediaType) =>
            Some(Content(Option(index), mediaType, contentToByteArray(zipFile, file)))
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

  def readResource(path: String, resourcePath: String): Option[Content] = {
    var zipFile: ZipFile = null

    try {
      zipFile = new ZipFile(path)

      zipFile.entries().asScala
        .filter(f => !f.isDirectory)
        .find(f => f.getName == resourcePath)
        .flatMap(f => FileUtil.getMediaType(f.getName) match {
          case Some(mediaType) =>
            Some(Content(None, mediaType, contentToByteArray(zipFile, f)))
          case None => None
        })
    } catch {
      case e: Throwable =>
        log.error("failed to read comic pages for " + path, e)
        None
    } finally {
      zipFile.close()
    }
  }
}
