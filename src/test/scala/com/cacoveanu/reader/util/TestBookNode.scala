package com.cacoveanu.reader.util

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.junit.jupiter.api.Test

import scala.io.{BufferedSource, Source}

class TestBookNode {

  val html = new String(Files.readAllBytes(Paths.get(getClass.getResource("/test1.html").toURI)), StandardCharsets.UTF_8)

  @Test
  def testTreeParsingDoesNotLoseInformation() = {
    val bodyStringOption = BookNode.getHtmlBody(html)
    assert(bodyStringOption.isDefined)
    val bodyString = bodyStringOption.get
    val treeOption = BookNode.parseHtmlTree(html)
    assert(treeOption.isDefined)
    val tree = treeOption.get
    assert(bodyString == tree.getContent())
  }
}
