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
    val treeOption = BookNode2.parse(bodyString)
    assert(treeOption.isDefined)
    val tree = treeOption.get
    assert(bodyString == tree.getContent())
  }

  @Test
  def testContentSize() = {
    val treeOption = BookNode.parseHtmlTree(html)
    assert(treeOption.isDefined)
    val tree = treeOption.get

    assert(tree.length() == 1075)
  }

  @Test
  def testCopySection() = {
    val expectedSubtree = """<h2>Parsing to tree</h2>
                            |  <p class="simple_text">Test if we can parse document to a tree of nodes. Leaves hold content. Leaves can be text nodes, images, empty tags and maybe tables.</p>""".stripMargin

    val treeOption = BookNode.parseHtmlTree(html)
    assert(treeOption.isDefined)
    val tree = treeOption.get

    tree.prettyPrint()
    //val subtree = tree.copy(355,507)
    //subtree.prettyPrint()
    //println(subtree.getContent())
    //assert(subtree.getContent() == expectedSubtree)
  }

  @Test
  def testSecondImplementation() = {
    val treeOption = BookNode.getHtmlBody(html).flatMap(BookNode2.parse(_))
    assert(treeOption.isDefined)
    val tree = treeOption.get

    tree.prettyPrint()
  }
}
