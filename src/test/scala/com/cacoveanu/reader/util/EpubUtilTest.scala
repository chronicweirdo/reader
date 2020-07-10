package com.cacoveanu.reader.util

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.cacoveanu.reader.entity.Content
import com.cacoveanu.reader.service.xml.ResilientXmlLoader
import org.junit.jupiter.api.Test
import com.cacoveanu.reader.util.HtmlUtil.AugmentedHtmlString
import com.cacoveanu.reader.util.HtmlUtil.AugmentedJsoupDocument

class EpubUtilTest {

  private val path1 = Paths.get("src", "test", "resources", "rembrandt.epub").toString
  private val path2 = Paths.get("src", "test", "resources", "pg1257.epub").toString
  private val path3 = Paths.get("src", "test", "resources", "pg16273-images.epub").toString

  @Test
  def readToc(): Unit = {
    println(path1)
    val toc = EpubUtil.getToc(path1)
    toc.foreach(println)
  }

  @Test
  def getCover() = {
    val cover: Option[Content] = EpubUtil.getCover(path1)
    assert(cover.isDefined)
  }

  @Test
  def getTocLink(): Unit = {
    println(EpubUtil.getTocLink(path1))
    println(EpubUtil.getTocLink(path2))
    println(EpubUtil.getTocLink(path3))
  }
}
