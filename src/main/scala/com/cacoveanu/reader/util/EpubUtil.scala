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

import scala.jdk.CollectionConverters._
import scala.xml.{Elem, Node, XML}

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
    log.debug(s"parsing section $sectionPath from book $epubPath")
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

  private def getIntOrDefault(s: String, d: Int): Int =
    try {
      s.toInt
    } catch {
      case e: Exception => d
    }

  private def getRecursiveTocFromNcx2(ncxPath: String, n: Node, level: Int = 0): Seq[(Int, String, String, Int)] = {
    var result = Seq[(Int, String, String, Int)]()
    result = result :+ (
      getIntOrDefault((n \ "@playOrder").text, -1),
      (n \ "navLabel" \ "text").text,
      getAbsoluteEpubPath(ncxPath, URLDecoder.decode((n \ "content" \ "@src").text, StandardCharsets.UTF_8.name())),
      level
    )
    (n \ "navPoint").foreach(n2 => {
      result = result ++ getRecursiveTocFromNcx2(ncxPath, n2, level + 1)
    })
    result
  }

  private def getTocFromNcx2(epubPath: String) = {
    getNcx(epubPath).map { case (ncxPath, ncx) =>
      (ncx \ "navMap" \ "navPoint")
        .flatMap(n => getRecursiveTocFromNcx2(ncxPath, n))
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
    val tocWithPositions = toc.flatMap { case (index, title, link, level) => {
      val bookTocEntry = new BookTocEntry
      bookTocEntry.index = index
      bookTocEntry.title = title
      bookTocEntry.level = level
      val position: Long = bookLinks.getOrElse(link, bookLinks.getOrElse(getRootLink(link), -1))
      if (position == -1) {
        None
      } else {
        bookTocEntry.position = position
        Some(bookTocEntry)
      }
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

  def getCoverFromOpf(epubPath: String): Option[Content] =
    getOpf(epubPath).flatMap { case (opfPath, opf) => getCoverResource(opfPath, opf)}
    .flatMap { case (href, contentType) =>
      readResource(epubPath, href)
        .map(bytes => Content(None, contentType, bytes))
    }

  def findCoverInResource(epubPath: String, resourcePath: String) = {
    readResource(epubPath, resourcePath).flatMap(getXml)
      .flatMap( xml => {
        val fromImg = (xml \\ "img").headOption.map(imgNode => getAbsoluteEpubPath(resourcePath, (imgNode \ "@src").text))
        if (fromImg.isDefined) {
          fromImg
        } else {
          val fromImage = (xml \\ "image").headOption.map(imgNode => getAbsoluteEpubPath(resourcePath, (imgNode \ "@{http://www.w3.org/1999/xlink}href").text))
          fromImage
        }
      })
      .flatMap( coverResource => readResource(epubPath, coverResource).map(bytes => Content(None, FileUtil.getMediaType(coverResource).getOrElse(null), bytes)))
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
