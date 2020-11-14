package com.cacoveanu.reader.util

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.junit.jupiter.api.Test

import scala.io.{BufferedSource, Source}

class TestBookNode {

  val html = new String(Files.readAllBytes(Paths.get(getClass.getResource("/test1.html").toURI)), StandardCharsets.UTF_8)

  private def parseTree() = {
    val treeOption = BookNode.getHtmlBody(html).flatMap(BookNode2.parse(_))
    assert(treeOption.isDefined)
    val tree = treeOption.get
    tree
  }

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

  private def inorderLeaves(node: BookNode2): Seq[BookNode2] = {
    if (node.children.isEmpty) Seq(node)
    else {
      node.children.flatMap(c => inorderLeaves(c))
    }
  }

  @Test
  def testNextLeaf() = {
    val tree = parseTree()
    tree.prettyPrint()

    val expectedLeaves = inorderLeaves(tree)

    var leaf = tree.nextLeaf()
    var i = 0
    while (leaf != null) {
      leaf.prettyPrint()
      assert(leaf == expectedLeaves(i))
      leaf = leaf.nextLeaf()
      i = i + 1
    }
  }

  private def getLastLeaf(node: BookNode2): BookNode2 = {
    var current = node;
    while (current.children.nonEmpty) current = current.children.last
    current
  }

  @Test
  def testPreviousLeaf() = {
    val tree = parseTree()
    val expectedLeaves = inorderLeaves(tree)
    var leaf = getLastLeaf(tree)
    var i = expectedLeaves.length - 1
    while (leaf != null) {
      leaf.prettyPrint()
      assert(leaf == expectedLeaves(i))
      leaf = leaf.previousLeaf()
      i = i - 1
    }
  }

  // each leaf is copied in the returned sequence * its length
  private def inorderLeavesWeighted(node: BookNode2): Seq[BookNode2] = {
    if (node.children.isEmpty) (0 until node.getLength()).map(_ => node)
    else {
      node.children.flatMap(c => inorderLeavesWeighted(c))
    }
  }

  @Test
  def testLeafAtPosition() = {
    val tree = parseTree()
    tree.prettyPrint()
    println()

    println(tree.getLength())
    val leavesAtPositions = inorderLeavesWeighted(tree)
    println(leavesAtPositions.size)

    for (i <- 0 until tree.getLength()) {
      val leaf = tree.leafAtPosition(i)
      assert(leaf == leavesAtPositions(i))
    }
  }
}
