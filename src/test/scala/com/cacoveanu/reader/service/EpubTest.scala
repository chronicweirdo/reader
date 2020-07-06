package com.cacoveanu.reader.service

import java.io.ByteArrayOutputStream
import java.util.zip.ZipFile

import scala.jdk.CollectionConverters._
import org.apache.tomcat.util.http.fileupload.IOUtils
import scala.xml._

object EpubTest {

  def readEpub(path: String) = {
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

  def main(args: Array[String]): Unit = {
    readEpub("Algorithms.epub")
  }

}
