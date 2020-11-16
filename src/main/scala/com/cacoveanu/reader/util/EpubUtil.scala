package com.cacoveanu.reader.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.zip.ZipFile

import com.cacoveanu.reader.entity.{Content, TocEntry}
import com.cacoveanu.reader.service.xml.ResilientXmlLoader
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.slf4j.{Logger, LoggerFactory}
import com.cacoveanu.reader.util.HtmlUtil.AugmentedHtmlString
import com.cacoveanu.reader.util.HtmlUtil.AugmentedJsoupDocument
import org.jsoup.nodes.{Document, Element, Node, TextNode}

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

  /*private def computeStartPositionOfElements(doc: Document) = {
    val body = doc.body()

    def comp(el: Node): Int = {
      if (el.isInstanceOf[TextNode]) {
        val text = el.asInstanceOf[TextNode].text()
        return text.length
      } else {
        var currentSize = 1
        for (child <- el.childNodes().asScala) {
          currentSize = currentSize + comp(child)
        }
        return currentSize
      }
    }
    comp(body)
  }*/

  private def getSectionSize(epubPath: String, sectionPath: String) = {
    val sectionExtension = FileUtil.getExtension(EpubUtil.baseLink(sectionPath))
    if (sectionExtension == "html" || sectionExtension == "xhtml" || sectionExtension == "htm" || sectionExtension == "xml") {
      EpubUtil.readResource(epubPath, EpubUtil.baseLink(sectionPath))
        //.flatMap(getXml)
        .map(bytes => new String(bytes, "UTF-8"))
        .map(text =>
          text.asHtml.bodyText.length
          /*computeStartPositionOfElements(text.asHtml)*/
        )
        //.map(html => (html \ "body").text.length)
        .getOrElse(-1)
    } else {
      1
    }
  }

  private def parseSection(epubPath: String, sectionPath: String, startPosition: Int = 0) = {
    val sectionExtension = FileUtil.getExtension(EpubUtil.baseLink(sectionPath))
    if (sectionExtension == "html" || sectionExtension == "xhtml" || sectionExtension == "htm" || sectionExtension == "xml") {
      EpubUtil.readResource(epubPath, EpubUtil.baseLink(sectionPath))
        .map(bytes => new String(bytes, "UTF-8"))
        .flatMap(text => BookNode.parse(text, startPosition))
    } else None
  }

  private def getTocFromOpf(epubPath: String) = {
    getOpf(epubPath).map { case (opfPath, opf) =>
      (opf \ "spine" \ "itemref")
        .map(n => (n \ "@idref").text)
        .flatMap(id =>
          (opf \ "manifest" \ "item")
            .find(n => (n \ "@id").text == id)
            .map(n => URLDecoder.decode((n \ "@href").text, StandardCharsets.UTF_8.name()))
        )
        .zipWithIndex
        .map(e => new TocEntry(
          TocEntry.OPF,
          e._2,
          e._1,
          getAbsoluteEpubPath(opfPath, e._1)
        ))
    }
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

  private def getTocFromNcx(epubPath: String) = {
    getNcx(epubPath).map { case (ncxPath, ncx) =>
      (ncx \ "navMap" \ "navPoint")
        .map(n => new TocEntry(
          TocEntry.NCX,
          (n \ "@playOrder").text.toInt,
          (n \ "navLabel" \ "text").text,
          getAbsoluteEpubPath(ncxPath, URLDecoder.decode((n \ "content" \ "@src").text, StandardCharsets.UTF_8.name()))
        ))
    }
  }

  // get TOC 2: the TOCening
  def getToc2(epubPath: String) = {
    val resources = getTocFromOpf2(epubPath).getOrElse(Seq())
    //println(resources)
    var parsedResources = Seq[(Int, String, BookNode)]()
    for (index <- resources.indices) {
      val startPosition = if (parsedResources.nonEmpty) parsedResources.last._3.end + 1 else 0
      val ps = parseSection(epubPath, resources(index)._2, startPosition)
      if (ps.isDefined) {
        parsedResources = parsedResources :+ (resources(index)._1, resources(index)._2, ps.get)
      }
    }
    //println(parsedResources)
    val resourcesWithPosition = parsedResources.map { case (i, str, node) => (node.start, node.end, str)}
    println("positions to resources:")
    resourcesWithPosition.foreach(println)
    println()
    var linkToPosition = Map[String, Int]()
    for (index <- parsedResources.indices) {
      linkToPosition = linkToPosition + (parsedResources(index)._2 -> parsedResources(index)._3.start)
      linkToPosition = linkToPosition ++ parsedResources(index)._3.getIds().map { case (id, pos) => (parsedResources(index)._2 + "#" + id, pos)}
    }
    println("links to positions:")
    linkToPosition.foreach(println)
    println()

    val toc = getTocFromNcx2(epubPath).getOrElse(Seq())
    //println(toc)
    println()
    val tocWithPositions = toc.map { case (index, title, link) =>
      (index, title, link, linkToPosition.getOrElse(link, linkToPosition.getOrElse(getRootLink(link), -1))) }
    println("table of contents to positions:")
    tocWithPositions.foreach(println)
    println()
  }

  def getRootLink(link: String) = {
    if (link.indexOf("#") >= 0) link.substring(0, link.indexOf("#"))
    else link
  }

  def getToc(epubPath: String) = {
    var toc = getTocFromOpf(epubPath).getOrElse(Seq()) ++ getTocFromNcx(epubPath).getOrElse(Seq())

    // compute sections sizes and find points of interest
    val sections: Seq[String] = getSections(toc).map(s => s.resource)
    var parsedSections = Seq[(String, BookNode)]()
    var contentToc = Seq[TocEntry]()
    for (i <- sections.indices) {
      val ps = parseSection(epubPath, sections(i), if (parsedSections.nonEmpty) parsedSections.last._2.end + 1 else 0)
      if (ps.isDefined) {
        parsedSections = parsedSections :+ (sections(i), ps.get)
        contentToc = contentToc :+ new TocEntry(TocEntry.CONTENT, ps.get.start, sections(i),
          sections(i))
        contentToc = contentToc ++ ps.get.getIds().map { case (id, position) => new TocEntry(TocEntry.CONTENT, position, id,
          sections(i) + "#" + id)}
      }
    }
    toc = toc.map(e => if (e.source == TocEntry.OPF) {
      parsedSections.find(_._1 == e.resource) match {
        case Some((resource, node)) =>
          e.start = node.start
          e.size = node.getLength()
          e
        case None => e
      }
    } else e)
    toc = toc ++ contentToc

    toc
  }

  def getSections(toc: Seq[TocEntry]) = {
    val ncxToc = toc.filter(_.source == TocEntry.OPF).sortBy(_.index)
    if (ncxToc.size > 1) {
      Seq(ncxToc(0)) ++ ncxToc.sliding(2)
        .filter(p => p(0).resource != p(1).resource)
        .map(p => p(1))
    } else ncxToc
  }

  /*def getSections(epubPath: String, toc: Seq[TocEntry]) = {
    val essentialToc = if (toc.size > 1) {
      Seq(toc(0)) ++ toc.sliding(2)
        .filter(p => EpubUtil.baseLink(p(0).link) != EpubUtil.baseLink(p(1).link))
        .map(p => p(1))
    } else toc

    var totalSize = 0
    val sections = essentialToc.map(e => {
      val basePath = baseLink(e.link)
      val sectionSize = getSectionSize(epubPath, basePath)
      val section = new Section(e.index, basePath, totalSize, sectionSize)
      totalSize = totalSize + sectionSize
      section
    })
    sections
  }*/

  // todo: rethink much of this and make book scanning more resilient to issues
  // todo: TOC is different from resources list, multiple TOC entries may be in a single resource
  /*def getToc(epubPath: String) = {
    val toc = getNcx(epubPath).map { case (opfPath, opf) =>
      (opf \ "navMap" \ "navPoint")
        .map(n => new Section(
          (n \ "@playOrder").text.toInt,
          (n \ "navLabel" \ "text").text,
          getAbsoluteEpubPath(opfPath, (n \ "content" \ "@src").text),
          -1,
          -1
        ))
    }.getOrElse(Seq())
    // remove parts of toc that are in the same file
    if (toc.size > 1) {
      val realToc = Seq(toc(0)) ++
        toc.sliding(2)
          .filter(p => EpubUtil.baseLink(p(0).link) != EpubUtil.baseLink(p(1).link))
          .map(p => p(1))
      var totalSize = 0
      val realTocWithSizes = realToc.map(e => {
        val sectionSize = getSectionSize(epubPath, e.link)
        val ne = new Section(e.index, e.title, e.link, totalSize, sectionSize)
        totalSize = totalSize + sectionSize
        ne
      })
      realTocWithSizes
    } else toc
  }*/

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
