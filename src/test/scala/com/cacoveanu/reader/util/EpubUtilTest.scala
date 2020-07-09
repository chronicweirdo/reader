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



  @Test
  def getSectionsWithPositionsAndSizes() = {
    val toc = EpubUtil.getToc(path)
    toc.foreach(t => println(t.link, t.size))
    /*
    Correct sizes:
    (OEBPS/@public@vhost@g@gutenberg@html@files@17215@17215-h@17215-h-0.htm.html#pgepubid00000,36235)
    // max position in browser: 37465
    (OEBPS/@public@vhost@g@gutenberg@html@files@17215@17215-h@17215-h-1.htm.html#pgepubid00012,40420)
    // max position in browser: 41568
     */
    //val rp = "OEBPS/@public@vhost@g@gutenberg@html@files@17215@17215-h@17215-h-0.htm.html#pgepubid00000"
    val rp = "OEBPS/@public@vhost@g@gutenberg@html@files@17215@17215-h@17215-h-1.htm.html#pgepubid00012"
    val t = new String(EpubUtil.readResource(path, EpubUtil.baseLink(rp)).get, "UTF-8")
    val t1 = t.asHtml.bodyText
    val t2 = (ResilientXmlLoader.loadString(t) \ "body").text
    println("size 1:" + t1.length)
    println("size 2:" + t2.length)
    val s3 = HtmlUtil.computeStartPositions(t.asHtml)
    println("size 3:" + (s3.last._1 + s3.last._2.length))
    Files.write(Paths.get("t1.txt"), t1.getBytes(StandardCharsets.UTF_8))
    Files.write(Paths.get("t2.txt"), t2.getBytes(StandardCharsets.UTF_8))

    val t3 = s3.map(e => e._1.toString + ": " + e._2).mkString("\n")
    Files.write(Paths.get("t3.txt"), t3.getBytes(StandardCharsets.UTF_8))

    println(t1==t2)



    // works and positions for this book are consistent with js
  }

  @Test
  def getToc2() = {
    val p = "D:\\books\\2020.06 books\\Kim Stanley Robinson - Mars Trilogy 1 - Red Mars.epub"
    val toc = EpubUtil.getToc(p)
    toc.foreach(t => println(t.index + " " + t.link + " " + t.size))
  }
}
