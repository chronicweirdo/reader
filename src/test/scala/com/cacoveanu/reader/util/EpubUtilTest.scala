package com.cacoveanu.reader.util

import java.nio.file.Paths

import com.cacoveanu.reader.entity.Content
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
}
