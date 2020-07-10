package com.cacoveanu.reader.util

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.cacoveanu.reader.entity.Content
import com.cacoveanu.reader.service.xml.ResilientXmlLoader
import org.junit.jupiter.api.Test
import com.cacoveanu.reader.util.HtmlUtil.AugmentedHtmlString
import com.cacoveanu.reader.util.HtmlUtil.AugmentedJsoupDocument

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

}
