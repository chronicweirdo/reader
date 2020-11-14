package com.cacoveanu.reader.util

import com.cacoveanu.reader.util.BookNode2.shouldBeLeafElement

import scala.util.matching.Regex

object BookNode2 {

  private val VOID_ELEMENTS = Seq("area","base","br","col","hr","img","input","link","meta","param","keygen","source")
  private val LEAF_ELEMENTS = Seq("img", "table")

  // if it starts and ends with angle brackets
  private def isTag(str: String) = "^</?[^>]+>$".r matches str
  private def isEndTag(str: String) = "^</[^>]+>$".r matches str
  private def isBothTag(str: String) = "^<[^>/]+/>$".r matches str
  private def getTagName(str: String) = "</?([^>\\s]+)".r findFirstMatchIn str match {
    case Some(m) =>
      m.group(1)
    case None => null
  }
  private def isVoidElement(tagName: String) = VOID_ELEMENTS.contains(tagName.toLowerCase())
  private def shouldBeLeafElement(tagName: String) = LEAF_ELEMENTS.contains(tagName.toLowerCase())

  private def parseBody(body: String): Option[BookNode2] = {
    val bodyNode = new BookNode2("body", "")
    var current: BookNode2 = bodyNode

    var content = ""

    for (i <- 0 until body.length) {
      val c = body(i)

      if (c == '<') {
        // starting a new tag
        // save what we have in content
        if (isTag(content)) throw new Throwable("this should not happen")
        else {
          // can only be a text node or nothing
          if (content.length > 0) {
            current.addChild(new BookNode2("text", content))
            content = ""
          }
        }
      }

      // accumulate content
      content += c

      if (c == '>') {
        // ending a tag
        if (isTag(content)) {
          val name = getTagName(content)
          // can only be a tag
          if (isEndTag(content)) {
            // we check that this tag closes the current node correctly
            if (isVoidElement(name)) {
              // the last child should have the correct name
              if (name != current.children.last.name) throw new Throwable("incompatible end for void tag")
              else {
                current.children.last.content += content
              }
            } else {
              // the current node should have the correct name, and it is getting closed
              if (name != current.name) throw new Throwable("incompatible end tag")
              // move current node up
              current = current.parent
            }
          } else if (isBothTag(content) || isVoidElement(name)) {
            // just add this tag without content
            current.addChild(new BookNode2(name, content))
          } else {
            // a start tag
            val newNode = new BookNode2(name, content)
            current.addChild(newNode)
            current = newNode
          }
          // reset content
          content = ""
        } else throw new Throwable("wild > encountered")
      }
    }
    // add the last text node, if there is still such a thing remaining
    if (content.nonEmpty) {
      if (isTag(content)) throw new Throwable("this should not happen")
      else {
        // can only be a text node or nothing
        if (content.length > 0) {
          current.addChild(new BookNode2("text", content))
          content = ""
        }
      }
    }

    bodyNode.collapseLeafs()
    bodyNode.updatePositions()
    Some(bodyNode)
  }

  def getHtmlBody(html: String): Option[String] = {
    val bodyStartPattern = "<body[^>]*>".r
    val bodyStartMatch: Option[Regex.Match] = bodyStartPattern findFirstMatchIn html

    val bodyEndPattern = "</body\\s*>".r
    val bodyEndMatch = bodyEndPattern findFirstMatchIn html

    (bodyStartMatch, bodyEndMatch) match {
      case (Some(sm), Some(em)) => Some(html.substring(sm.end, em.start))
      case _ => None
    }
  }

  def parse(html: String): Option[BookNode2] = {
    getHtmlBody(html).flatMap(body => parseBody(body))
  }
}

class BookNode2 {

  var name: String = _
  var parent: BookNode2 = _
  var children: Seq[BookNode2] = Seq()
  var content: String = _
  var start: Int = _
  var end: Int = _

  private def this(name: String, content: String) = {
    this()
    this.name = name
    this.content = content
  }

  private def this(name: String, content: String, start: Int, end: Int) = {
    this(name, content)
    this.start = start
    this.end = end
  }

  def getLength() = this.end - this.start + 1

  def addChild(child: BookNode2) = {
    children = children :+ child
    child.parent = this
  }

  private def printAtLevel(level: Int, text: String) = {
    for (i <- 0 until level) print("\t")
    println(text)
  }
  def prettyPrint(level: Int = 0): Unit = {
    printAtLevel(level, this.name + "[" + this.start + "," + this.end + "]: " + this.content)
    this.children.foreach(c => c.prettyPrint(level+1))
  }

  def getContent(): String = {
    if (this.name == "text") this.content
    else if (this.name == "body") this.children.map(_.getContent()).mkString("")
    else if (shouldBeLeafElement(this.name) && this.children.isEmpty) this.content
    else this.content + this.children.map(_.getContent()).mkString("") + "</" + this.name + ">"
  }

  private def collapseLeafs(): Unit = {
    if (shouldBeLeafElement(this.name) && this.children.nonEmpty) {
      // extract content from children
      this.content = this.getContent()
      this.children = Seq()
    } else {
      this.children.foreach(_.collapseLeafs())
    }
  }

  private def updatePositions(entrancePosition: Int = 0): Unit = {
    var position = entrancePosition
    this.start = position
    if (this.name == "text") {
      this.end = this.start + this.content.length - 1
    } else if (shouldBeLeafElement(this.name)) {
      // occupies a single position
      this.end = this.start
    } else if (this.children.isEmpty) {
      // an element without children, maybe used for spacing, should occupy a single position
      this.end = this.start
    } else {
      // compute for children and update
      for (i <- this.children.indices) {
        val child = this.children(i)
        child.updatePositions(position)
        position = child.end + 1
      }
      this.end = this.children.last.end
    }

  }

  def nextLeaf(): BookNode2 = {
    // is this a leaf?
    var current = this
    if (current.children.isEmpty) {
      // go up the parent line until we find next sibling
      var parent = current.parent
      while (parent != null && parent.children.indexOf(current) == parent.children.size - 1) {
        current = parent
        parent = current.parent
      }
      if (parent != null) {
        // we have the next sibling in current, must find first leaf
        current = parent.children(parent.children.indexOf(current) + 1)
      } else {
        // we have reached root, this was the last leaf, there is no other
        return null
      }
    }
    // find first child of the current node
    while (current.children.nonEmpty) {
      current = current.children.head
    }
    current
  }

  def previousLeaf(): BookNode2 = {
    var current = this
    var parent = current.parent
    while (parent != null && parent.children.indexOf(current) == 0) {
      // keep going up
      current = parent
      parent = current.parent
    }
    if (parent != null) {
      current = parent.children(parent.children.indexOf(current) - 1)
      // go down on the last child track
      while (current.children.nonEmpty) current = current.children.last
      current
    } else null
  }

  def leafAtPosition(position: Int): BookNode2 = {
    if (position < this.start || this.end < position) null
    else {
      var currentNode = this
      while (currentNode != null && currentNode.children.nonEmpty) {
        currentNode.children.find(c => c.start <= position && position <= c.end) match {
          case Some(nextNode) => currentNode = nextNode
          case None => currentNode = null
        }
      }
      currentNode
    }
  }

  def root(): BookNode2 = {
    var current = this
    while (current.parent != null) current = current.parent
    current
  }
  def documentStart(): Int = this.root.start
  def documentEnd(): Int = this.root.end

  def findSpaceAfter(position: Int): Int = {
    val spacePattern = "\\s".r
    // first get leaf at position
    var leaf = leafAtPosition(position)
    // for a text node, next space may be in the text node, next space character after position
    // if other kind of node, next space is the start of next leaf
    if (leaf != null && leaf.end == position) {
      // we need to look in the next node
      leaf = leaf.nextLeaf()
    }
    if (leaf != null && leaf.name == "text") {
      val searchStartPosition = if (position - leaf.start + 1 > 0) position - leaf.start + 1 else 0
      val m = spacePattern.findFirstMatchIn(leaf.content.substring(searchStartPosition))
      if (m.isDefined) {
        return m.get.start + position + 1
      }
    }
    if (leaf != null) leaf.end
    else documentEnd()
  }

  def findSpaceBefore(position: Int): Int = {
    val spacePattern = "\\s[^\\s]*$".r
    var leaf = leafAtPosition(position)
    if (leaf != null && leaf.name == "text") {
      val searchText = leaf.content.substring(0, position - leaf.start)
      val m = spacePattern.findFirstMatchIn(searchText)
      if (m.isDefined) {
        return m.get.start + leaf.start
      }
    }
    if (leaf != null) {
      leaf = leaf.previousLeaf()
    }
    if (leaf != null) leaf.end
    else documentStart()
  }

  def copy(from: Int, to: Int): BookNode2 = {
    if (this.name == "text") {
      if (from <= this.start && this.end <= to) {
        // this node is copied whole
        new BookNode2("text", this.content, this.start, this.end)
      } else if (from <= this.start && this.start <= to && to<= this.end) {
        // copy ends at this node
        new BookNode2(this.name, this.content.substring(0, to - this.start + 1), this.start, to)
      } else if (this.start <= from && from <= this.end && this.end <= to) {
        // copy starts at this node
        new BookNode2(this.name, this.content.substring(from - this.start), from, this.end)
      } else if (this.start <= from && to < this.end) {
        // we only copy part of this node
        new BookNode2(this.name, this.content.substring(from - this.start, to - this.start + 1), from, to)
      } else {
        null
      }
    } else if (shouldBeLeafElement(this.name)) {
      if (from <= this.start && this.end <= to) {
        // include element in selection
        new BookNode2(this.name, this.content, this.start, this.end)
      } else {
        null
      }
    } else {
      if (this.end < from || this.start > to) {
        // this node is outside the range and should not be copied
        null
      } else {
        val nn = new BookNode2(this.name, this.content)
        nn.children = this.children
          .map(_.copy(from, to))
          .filter(_ != null)
        nn.children.foreach(_.parent = nn)
        if (nn.children.isEmpty) {
          nn.start = this.start
          nn.end = this.end
        } else {
          nn.start = nn.children.head.start
          nn.end = nn.children.last.end
        }
        nn
      }
    }
  }
}
