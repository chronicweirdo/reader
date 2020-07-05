package com.cacoveanu.reader.util

import java.nio.file.Paths

import com.cacoveanu.reader.entity.Content
import com.cacoveanu.reader.service.xml.ResilientXmlLoader
import org.junit.jupiter.api.Test

class EpubUtilTest {

  private val path = Paths.get("src", "test", "resources", "rembrandt.epub").toString

  @Test
  def readToc(): Unit = {
    println(path)
    val toc = EpubUtil.getToc(path)
    toc.foreach(println)
  }

  @Test
  def getCover() = {
    val cover: Option[Content] = EpubUtil.getCover(path)
    assert(cover.isDefined)
  }

  private def readSectionAndComputeSize(epubPath: String, sectionPath: String) = {
    val bytes: Array[Byte] = EpubUtil.readResource(epubPath, EpubUtil.baseLink(sectionPath)).get
    val text = new String(bytes, "UTF-8")
    val html = ResilientXmlLoader.loadString(text)
    (html \ "body").text.length
  }

  @Test
  def getSectionsWithPositionsAndSizes() = {
    val toc = EpubUtil.getToc(path)
    val tocWithSizes = toc.map(e => {
      val section = e.link
      val size = readSectionAndComputeSize(path, section)
      (section, size)
    })
    println(tocWithSizes)
    // works and positions for this book are
  }
}
