package com.cacoveanu.reader.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.zip.ZipFile

import com.cacoveanu.reader.entity.{BookLink, BookResource, BookTocEntry, Content}
import com.cacoveanu.reader.service.xml.ResilientXmlLoader
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.IterableOnce.iterableOnceExtensionMethods
import scala.jdk.CollectionConverters._
import scala.xml.{Elem, XML}

object EpubUtil {

  private val log: Logger = LoggerFactory.getLogger(EpubUtil.getClass)
  private val OPF_REGEX = ".+\\.opf$"
  private val NCX_REGEX = ".+\\.ncx$"

  def readResource(epubPath: String, resourcePath: String): Option[Array[Byte]] = {
    val basePath = baseLink(resourcePath)
    var zipFile: ZipFile = null

    try {
      zipFile = new ZipFile(epubPath)
      zipFile.entries().asScala.find(e => e.getName == basePath).map(f => {
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
      if (zipFile != null) zipFile.close()
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
      if (zipFile != null) zipFile.close()
    }
  }

  private def getOpf(epubPath: String): Option[(String, Elem)] =
    findResource(epubPath, OPF_REGEX) match {
      case Some(opfPath) =>
        readResource(epubPath, opfPath).flatMap(getXml) match {
          case Some(xml) => Some((opfPath, xml))
          case None => None
        }
      case None => None
    }

  private def getNcx(epubPath: String) =
    findResource(epubPath, NCX_REGEX) match {
      case Some(ncxPath) =>
        readResource(epubPath, ncxPath).flatMap(getXml) match {
          case Some(xml) => Some((ncxPath, xml))
          case None => None
        }
      case None => None
    }

  def parseSection(epubPath: String, sectionPath: String, startPosition: Long = 0) = {
    val sectionExtension = FileUtil.getExtension(EpubUtil.baseLink(sectionPath))
    if (sectionExtension == "html" || sectionExtension == "xhtml" || sectionExtension == "htm" || sectionExtension == "xml") {
      EpubUtil.readResource(epubPath, EpubUtil.baseLink(sectionPath))
        .map(bytes => new String(bytes, "UTF-8"))
        .flatMap(text => BookNode.parse(text, startPosition))
    } else None
  }

  def getTocFromOpf2(epubPath: String) = {
    getOpf(epubPath).map { case (opfPath, opf) =>
      (opf \ "spine" \ "itemref")
        .map(n => (n \ "@idref").text)
        .flatMap(id =>
          (opf \ "manifest" \ "item")
            .find(n => (n \ "@id").text == id)
            .map(n => URLDecoder.decode((n \ "@href").text, StandardCharsets.UTF_8.name()))
        )
        .zipWithIndex
        .map(e => (
          e._2,
          getAbsoluteEpubPath(opfPath, e._1)
        ))
    }
  }

  private def getTocFromNcx2(epubPath: String) = {
    getNcx(epubPath).map { case (ncxPath, ncx) =>
      (ncx \ "navMap" \ "navPoint")
        .map(n => (
          (n \ "@playOrder").text.toInt,
          (n \ "navLabel" \ "text").text,
          getAbsoluteEpubPath(ncxPath, URLDecoder.decode((n \ "content" \ "@src").text, StandardCharsets.UTF_8.name()))
        ))
    }
  }

  // get TOC 2: the TOCening
  def scanContentMetadata(epubPath: String) = {
    // get book resources and links
    val resources = getTocFromOpf2(epubPath).getOrElse(Seq())

    var lastEnd: java.lang.Long = null
    var bookResources = Seq[BookResource]()
    var bookLinks = Map[String, Long]()
    for (index <- resources.indices) {
      val resourcePath = resources(index)._2
      val startPosition = if (lastEnd != null) lastEnd + 1 else 0
      val parsedSectionOptional = parseSection(epubPath, resourcePath, startPosition)
      if (parsedSectionOptional.isDefined) {
        val parsedSection = parsedSectionOptional.get
        lastEnd = parsedSection.end

        val bookResource = new BookResource()
        bookResource.start = parsedSection.start
        bookResource.end = parsedSection.end
        bookResource.path = resourcePath
        bookResources = bookResources :+ bookResource

        bookLinks = bookLinks + (resourcePath -> parsedSection.start)
        bookLinks = bookLinks ++ parsedSection.getIds().map { case (id, pos) => (resourcePath + "#" + id, pos)}
      }
    }

    val toc = getTocFromNcx2(epubPath).getOrElse(Seq())
    val tocWithPositions = toc.map { case (index, title, link) => {
      val bookTocEntry = new BookTocEntry
      bookTocEntry.index = index
      bookTocEntry.title = title
      val position: Long = bookLinks.getOrElse(link, bookLinks.getOrElse(getRootLink(link), -1))
      bookTocEntry.position = position
      bookTocEntry
    }}

    val bookLinkObjects = bookLinks.map { case (link, position) => {
      val bookLink = new BookLink
      bookLink.link = link
      bookLink.position = position
      bookLink
    }}.toSeq

    (bookResources, bookLinkObjects, tocWithPositions)
  }

  def getRootLink(link: String) = {
    if (link.indexOf("#") >= 0) link.substring(0, link.indexOf("#"))
    else link
  }

  def baseLink(link: String): String =
    if (link.indexOf("#") >= 0) link.substring(0, link.indexOf("#"))
    else link

  def getTitle(epubPath: String): Option[String] =
    getOpf(epubPath).flatMap { case (_, opf) => (opf \ "metadata" \ "title").headOption.map(_.text) }

  def getAuthor(epubPath: String): Option[String] =
    getOpf(epubPath).flatMap { case (_, opf) => (opf \ "metadata" \ "creator").headOption.map(_.text) }

  def getTocLink(epubPath: String): Option[String] =
    getOpf(epubPath) match {
      case Some((opfPath, opf)) =>
        (opf \ "guide" \ "reference")
          .find(n => (n \ "@type").text == "toc")
          .map(n => (n \ "@href").text)
          .map(link => getAbsoluteEpubPath(opfPath, link))
      case None => None
    }

  private def getCoverResource(opfPath: String, contentOpf: Elem): Option[(String, String)] = {
    val coverId = (contentOpf \ "metadata" \ "meta")
      .find(n => (n \ "@name").text == "cover")
      .map(n => (n \ "@content").text)

    val coverResource = coverId.flatMap(id => {
        (contentOpf \\ "manifest" \ "item")
          .find(node => (node \ "@id").text == id || (node \ "@properties").text == id)
          .map(node => (
            getAbsoluteEpubPath(opfPath, (node \ "@href").text),
            (node \ "@media-type").text)
          )
      })
    coverResource
  }

  def getCover(epubPath: String): Option[Content] =
    getOpf(epubPath).flatMap { case (opfPath, opf) => getCoverResource(opfPath, opf)}
    .flatMap { case (href, contentType) =>
      readResource(epubPath, href)
        .map(bytes => Content(None, contentType, bytes))
    }

  def getAbsoluteEpubPath(povPath: String, currentPath: String): String = {
    // check if current path is absolute
    if (currentPath.startsWith("/")) return currentPath
    // get the folder path of the povPath
    val folderPath = if (povPath.lastIndexOf("/") >= 0) povPath.substring(0, povPath.lastIndexOf("/")) else null
    val newPath = if (folderPath != null) folderPath + "/" + currentPath else currentPath
    val normalizedPath = Paths.get(newPath).normalize().toString.replaceAll("\\\\", "/")
    normalizedPath
  }

  def getXml(data: Array[Byte]) = {
    try {
      Some(ResilientXmlLoader.load(new ByteArrayInputStream(data)))
    } catch {
      case t: Throwable =>
        log.warn(s"failed to parse xml", t)
        None
    }
  }
}
