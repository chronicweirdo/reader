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
  def testGetResources() = {
    val tree = parseTree()
    val resources = tree.getResources()
    assert(resources.size == 4)
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

  @Test
  def testNextTraversal(): Unit = {
    val expectedNodes = Array(Array("body", 0, 1111),Array("text", 0, 3),Array("p", 4, 23),Array("text", 4, 23),Array("text", 24, 27),Array("p", 28, 350),Array("text", 28, 350),Array("text", 351, 354),Array("h2", 355, 369),Array("text", 355, 369),Array("text", 370, 373),Array("p", 374, 506),Array("text", 374, 506),Array("text", 507, 510),Array("img", 511, 511),Array("text", 512, 515),Array("p", 516, 627),Array("text", 516, 627),Array("text", 628, 631),Array("p", 632, 731),Array("text", 632, 731),Array("text", 732, 735),Array("div", 736, 736),Array("text", 737, 740),Array("h2", 741, 770),Array("text", 741, 770),Array("text", 771, 774),Array("p", 775, 859),Array("text", 775, 792),Array("a", 793, 799),Array("text", 793, 799),Array("text", 800, 859),Array("text", 860, 863),Array("p", 864, 907),Array("text", 864, 907),Array("text", 908, 911),Array("img", 912, 912),Array("text", 913, 916),Array("h2", 917, 931),Array("text", 917, 931),Array("text", 932, 935),Array("img", 936, 936),Array("text", 937, 940),Array("p", 941, 1082),Array("text", 941, 1082),Array("text", 1083, 1086),Array("table", 1087, 1109),Array("text", 1087, 1092),Array("tr", 1093, 1093),Array("text", 1094, 1099),Array("tr", 1100, 1100),Array("text", 1101, 1106),Array("tr", 1107, 1107),Array("text", 1108, 1109),Array("text", 1110, 1111))

    val tree = parseTree()
    var current = tree
    var index = 0
    while (current != null) {
      println(current.name + " " + current.start + " " + current.end)
      assert(current.name == expectedNodes(index)(0))
      assert(current.start == expectedNodes(index)(1))
      assert(current.end == expectedNodes(index)(2))
      current = current.nextNode()
      index = index + 1
    }
    assert(index == 55)
  }

  @Test
  def textNextHeader(): Unit = {
    val tree = parseTree()
    val p1 = tree.leafAtPosition(632)
    val h1 = p1.nextNodeOfName("h2")
    assert(h1 != null)
    println(h1.name + " " + h1.start + " " + h1.end)
    assert(h1.start == 741)
    val h2 = h1.nextNodeOfName("h2")
    assert(h2 != null)
    println(h2.name + " " + h2.start + " " + h2.end)
    assert(h2.start == 917)

    val p2 = tree.leafAtPosition(1108)
    val h3 = p2.nextNodeOfName("h2")
    assert(h3 == null)
  }
}
