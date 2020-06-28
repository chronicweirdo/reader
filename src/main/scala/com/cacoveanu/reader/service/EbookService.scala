package com.cacoveanu.reader.service

import java.io.ByteArrayOutputStream
import java.net.{URI, URL, URLEncoder}
import java.nio.file.{LinkOption, Paths}
import java.util.zip.ZipFile

import org.apache.tomcat.util.http.fileupload.IOUtils
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

import scala.jdk.CollectionConverters._
import scala.xml._
import scala.xml.transform._


object EbookService {
  val log: Logger = LoggerFactory.getLogger(classOf[EbookService])

  def main(args: Array[String]): Unit = {
    val service = new EbookService

    val path = "text/part0004.html"
    val data = service.readFromEpub("Algorithms.epub", path)
    data.foreach(b => {
      val html = new String(b)
      val newHtml = service.processHtml(html, path)
      println(newHtml)
    })
  }
}

class LinkRewriteRule(bookId: String, htmlPath: String) extends RewriteRule {

  private def getFolder(path: String) = {
    val lio = path.lastIndexOf("/")
    if (lio > 0) path.substring(0, lio)
    else ""
  }

  private val folder = getFolder(htmlPath)

  private def splitPath(path: String) = {
    val di = path.lastIndexOf("#")
    if (di > 0) (path.substring(0, di), path.substring(di+1))
    else (path, null)
  }

  private def getNewLink(remotePath: String): String = {
    println(">>> " + remotePath)
    val remoteUri = new URI(remotePath)
    if (remoteUri.isAbsolute) {
      remotePath
    } else {
      val (externalPath, internalPath) = splitPath(remotePath)
      val remotePathWithFolder = if (folder.length > 0) folder + "/" + externalPath else externalPath
      val normalizedPath = Paths.get(remotePathWithFolder).normalize().toString.replaceAll("\\\\", "/")
      s"book?id=$bookId&path=${URLEncoder.encode(normalizedPath, "UTF-8")}" + (if (internalPath != null) "#" + internalPath else "")
      //s"book?id=$bookId&path=$normalizedPath"
    }
  }

  private def extractHrefString(elem: Elem) = elem.attribute("href").map(c => c.head).map(n => n.toString()).getOrElse("")

  private def extractSrcString(elem: Elem) = elem.attribute("src").map(c => c.head).map(n => n.toString()).getOrElse("")

  private def transformElementWithHref(e: Node): Node = {
    val original: Elem = e.asInstanceOf[Elem]
    val originalHref = extractHrefString(original)
    val newHref = getNewLink(originalHref)
    original % Attribute("href", Unparsed(newHref), Null)
  }

  private def transformElementWithSrc(e: Node): Node = {
    val original: Elem = e.asInstanceOf[Elem]
    val originalSrc = extractSrcString(original)
    val newSrc = getNewLink(originalSrc)
    original % Attribute("src", Unparsed(newSrc), Null)
  }

  override def transform(n: Node) = n match {
    case e @ <a>{_*}</a> => transformElementWithHref(e)
    case e @ <link>{_*}</link> => transformElementWithHref(e)
    case e @ <img>{_*}</img> => transformElementWithSrc(e)
    case _ => n
  }
}

@Service
class EbookService {

  import EbookService.log

  private def readEpub(path: String) = {
    var zipFile: ZipFile = null

    try {
      zipFile = new ZipFile(path)
      zipFile.entries().asScala.foreach(println)
      val ncx: Option[String] = zipFile.entries().asScala.find(e => e.getName.endsWith(".ncx")).map(f => {
        val fileContents = zipFile.getInputStream(f)
        val bos = new ByteArrayOutputStream()
        IOUtils.copy(fileContents, bos)
        new String(bos.toByteArray)
      })
      //ncx.foreach(println)
      ncx match {
        case Some(n) =>
          (XML.loadString(n) \\ "navPoint").foreach(println)
        case None =>
      }
    } catch {
      case e: Throwable =>
        e.printStackTrace()
    } finally {
      zipFile.close()
    }
  }

  /*private val hrefRule = new RewriteRule {
    override def transform(n: Node) = n match {
      case e @ <a>{_*}</a> => e.asInstanceOf[Elem] %
        Attribute(null, "attr1", "200",
          Attribute(null, "attr2", "100", Null))
      case _ => n
    }
  }*/

  private def processHtml(html: String, path: String): String = {
    // make XML
    val xml = XML.loadString(html)
    // adjust all links


    val xml2 = new RuleTransformer(new LinkRewriteRule("1", path)).transform(xml)

    xml2.toString()
  }

  private def readFromEpub(epubPath: String, resourcePath: String): Option[Array[Byte]] = {
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

  private def getFiletype(path: String) = {
    val dot = path.lastIndexOf(".")
    val extension = path.substring(dot + 1).toLowerCase()
    extension match {
      case "html" => MediaType.TEXT_HTML_VALUE
      case "jpg" => MediaType.IMAGE_JPEG_VALUE
      case "jpeg" => MediaType.IMAGE_JPEG_VALUE
      case "gif" => MediaType.IMAGE_GIF_VALUE
      case "png" => MediaType.IMAGE_PNG_VALUE
      case "css" => "text/css"
    }
  }

  def loadResource(bookId: String, resourcePath: String): Option[(String, Array[Byte])] = {
    readFromEpub("Algorithms.epub", resourcePath) match {
      case Some(bytes) =>
        getFiletype(resourcePath) match {
          case "text/html" =>
            val data: Array[Byte] = processHtml(new String(bytes, "UTF-8"), resourcePath)
              .getBytes("UTF-8")
            Some(("text/html", data))
          case "text/css" => Some(("text/css", bytes))
          case "image/jpeg" => Some(("image/jpeg", bytes))
          case "image/png" => Some(("image/png", bytes))
          case "image/gif" => Some(("image/gif", bytes))
          case _ => None
        }
      case None => None
    }
  }
}
