package com.cacoveanu.reader.service

import java.io.ByteArrayOutputStream
import java.net.URLEncoder
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

    val data = service.readFromEpub("Algorithms.epub", "text/part0001.html")
    data.foreach(b => {
      val html = new String(b)
      val newHtml = service.processHtml(html)
      println(newHtml)
    })
  }
}

class HrefRewriteRule(bookId: String) extends RewriteRule {
  override def transform(n: Node) = n match {
    case e @ <a>{_*}</a> =>
      val original: Elem = e.asInstanceOf[Elem]
      //println("!!! " + original.attribute("href"))//.foreach(n => println("!!!" + n.asInstanceOf[Attribute].value))
      val originalHref = original.attribute("href").map(c => c.head).map(n => n.toString()).getOrElse("")
      println("!!! " + originalHref)
      // is relative or absolute path? only change relative paths
      // is path relative to the root of the book archive, or to some subfolder? make it relative to root!
      val newHref = s"book?id=$bookId&path=${URLEncoder.encode(originalHref, "UTF-8")}"
      //original % Attribute(null, "attr1", "200", Attribute(null, "attr2", "100", Null))
      original % Attribute(null, "href", newHref, Null)
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

  private def processHtml(html: String): String = {
    // make XML
    val xml = XML.loadString(html)
    // adjust all links


    val xml2 = new RuleTransformer(new HrefRewriteRule("1")).transform(xml)

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
