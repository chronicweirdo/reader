package com.cacoveanu.reader.service

import java.io.ByteArrayOutputStream
import java.net.{URI, URL, URLEncoder}
import java.nio.file.{LinkOption, Paths}
import java.util.zip.ZipFile

import org.apache.tomcat.util.http.fileupload.IOUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._
import scala.xml._
import scala.xml.transform._

object EbookService {
  val log: Logger = LoggerFactory.getLogger(classOf[EbookService])

  def main(args: Array[String]): Unit = {
    val service = new EbookService

    val path = "text/part0001.html"
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

  private def getNewLink(remotePath: String): String = {
    val remoteUri = new URI(remotePath)
    if (remoteUri.isAbsolute) {
      remotePath
    } else {
      val remotePathWithFolder = if (folder.length > 0) folder + "/" + remotePath else remotePath
      val normalizedPath = Paths.get(remotePathWithFolder).normalize().toString
      s"book?id=$bookId&path=${URLEncoder.encode(normalizedPath, "UTF-8")}"
    }
  }

  private def extractHrefString(elem: Elem) = elem.attribute("href").map(c => c.head).map(n => n.toString()).getOrElse("")

  private def transformElementWithHref(e: Node): Node = {
    val original: Elem = e.asInstanceOf[Elem]
    val originalHref = extractHrefString(original)
    val newHref = getNewLink(originalHref)
    original % Attribute("href", Unparsed(newHref), Null)
  }

  override def transform(n: Node) = n match {
    case e @ <a>{_*}</a> => transformElementWithHref(e)
    case e @ <link>{_*}</link> => transformElementWithHref(e)
    case _ => n
  }
}

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
}
