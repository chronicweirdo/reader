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

  private def getSectionSize(epubPath: String, sectionPath: String) = {
    val sectionExtension = FileUtil.getExtension(EpubUtil.baseLink(sectionPath))
    if (sectionExtension == "html" || sectionExtension == "xhtml" || sectionExtension == "htm") {
      EpubUtil.readResource(epubPath, EpubUtil.baseLink(sectionPath))
        //.flatMap(getXml)
        .map(bytes => new String(bytes, "UTF-8"))
        .map(text => text.asHtml.bodyText.length)
        //.map(html => (html \ "body").text.length)
        .getOrElse(-1)
    } else {
      1
    }
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
          false,
          e._2,
          e._1,
          getAbsoluteEpubPath(opfPath, e._1)
        ))
    }
  }

  private def getTocFromNcx(epubPath: String) = {
    getNcx(epubPath).map { case (ncxPath, ncx) =>
      (ncx \ "navMap" \ "navPoint")
        .map(n => new TocEntry(
          true,
          (n \ "@playOrder").text.toInt,
          (n \ "navLabel" \ "text").text,
          getAbsoluteEpubPath(ncxPath, URLDecoder.decode((n \ "content" \ "@src").text, StandardCharsets.UTF_8.name()))
        ))
    }
  }

  // todo: need to get the toc from the opf file, ncs is not reliable enough, don't know what to do about titles
  // todo: also, try to get the title from NCX, or otherwise from the linked documents themselves?
  def getToc(epubPath: String) = {
    /*val toc = getOpf(epubPath).map { case (opfPath, opf) =>
      (opf \ "spine" \ "itemref")
        .map(n => (n \ "@idref").text)
        .flatMap(id =>
          (opf \ "manifest" \ "item")
            .find(n => (n \ "@id").text == id)
            .map(n => (n \ "@href").text)
        )
        .zipWithIndex
        .map(e => new TocEntry(
          e._2,
          e._1,
          getAbsoluteEpubPath(opfPath, e._1)
        ))
    }.getOrElse(Seq())*/

    /*val toc = getNcx(epubPath).map { case (opfPath, opf) =>
      (opf \ "navMap" \ "navPoint")
        .map(n => new TocEntry(
          (n \ "@playOrder").text.toInt,
          (n \ "navLabel" \ "text").text,
          getAbsoluteEpubPath(opfPath, (n \ "content" \ "@src").text)
        ))
    }.getOrElse(Seq())*/
    val toc = getTocFromOpf(epubPath).getOrElse(Seq()) ++ getTocFromNcx(epubPath).getOrElse(Seq())

    val sections = getSections(toc).map(s => s.resource)
    var totalSize = 0
    val sectionSizes: Seq[(String, Int, Int)] = sections.map(section => {
      val sectionSize = getSectionSize(epubPath, section)
      val result = (section, totalSize, sectionSize)
      totalSize = totalSize + sectionSize
      result
    })

    val tocWithSizes = toc.map(e => if (!e.fromToc) {
      sectionSizes.find(_._1 == e.resource) match {
        case Some((resource, start, size)) =>
          e.start = start
          e.size = size
          e
        case None => e
      }
    } else e)
    tocWithSizes
  }

  def getSections(toc: Seq[TocEntry]) = {
    val ncxToc = toc.filter(_.fromToc == false).sortBy(_.index)
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
          .find(node => (node \ "@id").text == id )
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
