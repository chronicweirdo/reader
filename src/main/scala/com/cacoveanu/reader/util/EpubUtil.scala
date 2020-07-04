package com.cacoveanu.reader.util

import java.io.ByteArrayOutputStream
import java.util.zip.ZipFile

import com.cacoveanu.reader.entity.{Content, TocEntry}
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._
import scala.xml.{Elem, XML}

object EpubUtil {

  private val log: Logger = LoggerFactory.getLogger(EpubUtil.getClass)
  private val CONTENT_REGEX = ".+\\.opf$"

  def readResource(epubPath: String, resourcePath: String): Option[Array[Byte]] = {
    var zipFile: ZipFile = null

    try {
      zipFile = new ZipFile(epubPath)
      zipFile.entries().asScala.find(e => e.getName == resourcePath).map(f => {
        val fileContents = zipFile.getInputStream(f)
        val bos = new ByteArrayOutputStream()
        IOUtils.copy(fileContents, bos)
        bos.toByteArray
      })
    } catch {
      case e: Throwable =>
        log.error(s"failed to read epub $epubPath", e)
        None
    } finally {
      zipFile.close()
    }
  }

  def findResource(epubPath: String, resourceRegex: String): Option[String] = {
    var zipFile: ZipFile = null
    val pattern = resourceRegex.r

    try {
      zipFile = new ZipFile(epubPath)
      zipFile.entries().asScala
        .find(e => pattern.pattern.matcher(e.getName.toLowerCase).matches)
        .map(f => f.getName)
    } catch {
      case e: Throwable =>
        log.error(s"failed to read epub $epubPath", e)
        None
    } finally {
      zipFile.close()
    }
  }

  private def getOpf(epubPath: String) =
    findResource(epubPath, CONTENT_REGEX)
    .flatMap(readResource(epubPath, _))
    .flatMap(getXml)

  def getToc(epubPath: String) = {
    getOpf(epubPath).map(opf =>
      (opf \\ "navPoint")
        .map(n => TocEntry(
          (n \\ "@playOrder").text.toInt,
          (n \\ "text").text,
          (n \\ "@src").text
        ))
    ).getOrElse(Seq())
  }

  def getTitle(epubPath: String): Option[String] =
    getOpf(epubPath).flatMap(opf => (opf \ "metadata" \ "title").headOption.map(_.text))

  def getAuthor(epubPath: String): Option[String] =
    getOpf(epubPath).flatMap(opf => (opf \ "metadata" \ "creator").headOption.map(_.text))

  private def getCoverResource(contentOpf: Elem): Option[(String, String)] = {
    (contentOpf \ "metadata" \ "meta")
      .find(n => (n \ "@name").text == "cover")
      .map(n => (n \ "@content").text)
      .flatMap(id => {
        (contentOpf \\ "manifest" \ "item")
          .find(node => (node \ "@id").text == id )
          .map(node => ((node \ "@href").text, (node \ "@media-type").text))
      })
  }

  def getCover(epubPath: String): Option[Content] =
    getOpf(epubPath).flatMap(getCoverResource)
    .flatMap { case (href, contentType) =>
      findResource(epubPath, href)
        .flatMap(readResource(epubPath, _))
        .map(bytes => Content(None, contentType, bytes))
    }

  private def getXml(data: Array[Byte]) = {
    try {
      Some(XML.loadString(new String(data, "UTF-8")))
    } catch {
      case t: Throwable =>
        log.warn(s"failed to parse xml", t)
        None
    }
  }
}
