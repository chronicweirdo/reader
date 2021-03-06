package com.cacoveanu.reader.util

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test

import scala.io.{BufferedSource, Source}

class TestBookNode {

  val html = new String(Files.readAllBytes(Paths.get(getClass.getResource("/test1.html").toURI)), StandardCharsets.UTF_8)

  private def parseTree() = {
    val treeOption = BookNode.parse(html)
    assert(treeOption.isDefined)
    val tree = treeOption.get
    tree
  }

  @Test
  def testTreeParsingDoesNotLoseInformation() = {
    val bodyStringOption = BookNode.getHtmlBody(html)
    assert(bodyStringOption.isDefined)
    val bodyString = bodyStringOption.get
    val tree = parseTree()
    assert(bodyString == tree.extractContent())
  }

  @Test
  def testContentSize() = {
    val tree = parseTree()
    tree.prettyPrint()

    assert(tree.getLength() == 1112)
  }

  @Test
  def testStartWithPosition() = {
    val treeOption = BookNode.parse(html, 10)
    assert(treeOption.isDefined)
    val tree = treeOption.get

    assert(tree.start == 10)
    assert(tree.end == 1121)
  }


  private def inorderLeaves(node: BookNode): Seq[BookNode] = {
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

  private def getLastLeaf(node: BookNode): BookNode = {
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
  private def inorderLeavesWeighted(node: BookNode): Seq[BookNode] = {
    if (node.children.isEmpty) (0 until node.getLength().toInt).map(_ => node)
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

    for (i <- 0 until tree.getLength().toInt) {
      val leaf = tree.leafAtPosition(i)
      assert(leaf == leavesAtPositions(i.toInt))
    }
  }

  @Test
  def testFindSpaceAfter() = {
    val tree = parseTree()
    tree.prettyPrint()
    println()

    var space = tree.findSpaceAfter(0)
    while (space != tree.getDocumentEnd()) {
      println(space)
      space = tree.findSpaceAfter(space)
    }
    // we should be able to go over the whole document through conseccutive spaces
    assert(space == tree.getDocumentEnd())
  }

  @Test
  def testFindSpaceBefore() = {
    val tree = parseTree()
    tree.prettyPrint()
    println()

    var space = tree.findSpaceBefore(tree.getDocumentEnd())

    while (space != 0) {
      println(space)
      space = tree.findSpaceBefore(space)
    }
    // we should be able to go over the whole document through preceeding spaces
    assert(space == tree.getDocumentStart())
  }

  @Test
  def testFindSpacesBothWays() = {
    // going over the document with spaces before and spaces after, we should see the same spaces
    // a space, in our case, is a place where we can end a page
    // when searching for pages, we increase or decrease the page to the next space and previous space respectively
    // we do this until our page fits the view without going overt (triggering overflow)

    val tree = parseTree()
    tree.prettyPrint()
    println()

    var spacesAfter = Seq[Long]()
    var spacesBefore = Seq[Long]()

    var space = tree.findSpaceAfter(tree.getDocumentStart())
    while (space != tree.getDocumentEnd()) {
      spacesAfter = spacesAfter :+ space
      space = tree.findSpaceAfter(space)
    }
    //spacesAfter = spacesAfter :+ tree.documentEnd()

    space = tree.findSpaceBefore(tree.getDocumentEnd())
    while (space != tree.getDocumentStart()) {
      spacesBefore = space +: spacesBefore
      space = tree.findSpaceBefore(space)
    }
    //spacesBefore = tree.documentStart() +: spacesBefore

    println(spacesAfter)
    println(spacesBefore)

    assert(spacesAfter == spacesBefore)
  }

  @Test
  def testCopySection() = {
    val expectedSubtree = """<h2 id="ch1s1">Parsing to tree</h2>
                            |  <p class="simple_text">Test if we can parse document to a tree of nodes. Leaves hold content. Leaves can be text nodes, images, empty tags and maybe tables.</p>""".stripMargin

    val tree = parseTree()
    tree.prettyPrint()

    val part = tree.copy(355, 506)
    assert(part.extractContent() == expectedSubtree)
  }

  @Test
  def testCopy() = {
    val tree = parseTree()
    tree.prettyPrint()
    println()

    //val copy = tree.copy(2, 40)
    //copy.prettyPrint()

    val middleChildIndex = tree.children.size / 2
    val middleChildEnd = tree.children(middleChildIndex).end

    val copy1 = tree.copy(tree.start, middleChildEnd)
    copy1.prettyPrint()
    val copy2 = tree.copy(middleChildEnd+1, tree.end)
    copy2.prettyPrint()

    assert(copy1.extractContent() + copy2.extractContent() == tree.extractContent())
  }

  @Test
  def testCopy2() = {
    val tree = parseTree()
    tree.prettyPrint()
    println()

    val splitAndMergedContent = tree.children
      .map(c => tree.copy(c.start, c.end))
      .map(c => c.extractContent())
      .mkString("")

    assert(splitAndMergedContent == tree.extractContent())
  }

  @Test
  def testGetIds(): Unit = {
    val tree = parseTree()
    val ids = tree.getIds()
    println(ids)

    assert(ids.size == 8)
    assert(ids("ch1") < ids("ch1s1"))
    assert(ids("ch1s1") < ids("ch1im1"))
    assert(ids("ch1im1") < ids("ch1s2"))
    assert(ids("ch1s2") < ids("ch1im2"))
    assert(ids("ch1im2") < ids("ch1s3"))
    assert(ids("ch1s3") < ids("ch1im3"))
    assert(ids("ch1im3") < ids("ch1tbl1"))
  }

  @Test
  def testToJson(): Unit = {
    val tree = parseTree()
    val mapper = new ObjectMapper();

    val treeString = mapper.writeValueAsString(tree)
    println(treeString)
    assert(treeString != null)
    assert(treeString.length > 0)
  }
}
