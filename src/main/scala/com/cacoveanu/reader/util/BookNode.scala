package com.cacoveanu.reader.util

import com.cacoveanu.reader.util.BookNode.{getId, shouldBeLeafElement}
import com.fasterxml.jackson.annotation.{JsonBackReference, JsonIgnore}

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

object BookNode {

  private val VOID_ELEMENTS = Seq("area","base","br","col","hr","img","input","link","meta","param","keygen","source","image")
  private val LEAF_ELEMENTS = Seq("img", "tr", "image")

  // if it starts and ends with angle brackets
  private def isTag(str: String) = "^</?[^>]+>$".r matches str
  private def isComment(str: String) = "^<!\\-\\-[^>]+\\-\\->$".r matches str
  private def isEndTag(str: String) = "^</[^>]+>$".r matches str
  private def isBothTag(str: String) = "^<[^>/]+/>$".r matches str
  private def getTagName(str: String) = "</?([^>\\s]+)".r findFirstMatchIn str match {
    case Some(m) =>
      m.group(1)
    case None => null
  }
  private def getId(str: String) = "<[^>\\s]+.*id=\"([^\"\\s]+)\".*>".r findFirstMatchIn str match {
    case Some(m) =>
      Some(m.group(1))
    case None => None
  }
  private def isVoidElement(tagName: String) = VOID_ELEMENTS.contains(tagName.toLowerCase())
  private def shouldBeLeafElement(tagName: String) = LEAF_ELEMENTS.contains(tagName.toLowerCase())

  private def parseBody(body: String, entrancePosition: Long): Option[BookNode] = {
    val bodyNode = new BookNode("body", "")
    var current: BookNode = bodyNode

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
            current.addChild(new BookNode("text", content))
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
          // if it's comment, ignore
          if (isComment(content)) {
            // ignoring comment
          } else if (isEndTag(content)) {
            // we check that this tag closes the current node correctly
            if (isVoidElement(name)) {
              // the last child should have the correct name
              if (name != current.children.last.name) throw new Throwable("incompatible end for void tag")
              else {
                current.children.last.content += content
              }
            } else {
              // the current node should have the correct name, and it is getting closed
              if (name != current.name) throw new Throwable("incompatible end tag " + current.name)
              // move current node up
              current = current.parent
            }
          } else if (isBothTag(content) || isVoidElement(name)) {
            // just add this tag without content
            current.addChild(new BookNode(name, content))
          } else {
            // a start tag
            val newNode = new BookNode(name, content)
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
        current.addChild(new BookNode("text", content))
      }
    }

    bodyNode.collapseLeaves()
    bodyNode.updatePositions(entrancePosition)
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

  def parse(html: String, entrancePosition: Long = 0): Option[BookNode] = {
    getHtmlBody(html).flatMap(body => parseBody(body, entrancePosition))
  }
}

class BookNode {

  @BeanProperty
  var name: String = _
  @JsonBackReference
  @BeanProperty
  var parent: BookNode = _

  var children: Seq[BookNode] = Seq()
  @BeanProperty
  var content: String = _
  @BeanProperty
  var start: Long = _
  @BeanProperty
  var end: Long = _

  def getChildren() = {
   this.children.asJava
  }

  private def this(name: String, content: String) = {
    this()
    this.name = name
    this.content = content
  }

  private def this(name: String, content: String, start: Long, end: Long) = {
    this(name, content)
    this.start = start
    this.end = end
  }

  def getLength() = this.end - this.start + 1

  def addChild(child: BookNode) = {
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

  @JsonIgnore
  def extractContent(): String = {
    if (this.name == "text") this.content
    else if (this.name == "body") this.children.map(_.extractContent()).mkString("")
    else if (shouldBeLeafElement(this.name) && this.children.isEmpty) this.content
    else this.content + this.children.map(_.extractContent()).mkString("") + "</" + this.name + ">"
  }

  private def collapseLeaves(): Unit = {
    if (shouldBeLeafElement(this.name) && this.children.nonEmpty) {
      // extract content from children
      this.content = this.extractContent()
      this.children = Seq()
    } else {
      this.children.foreach(_.collapseLeaves())
    }
  }

  def getResources(): Seq[String] = {
    if (this.name == "img") {
      val srcMatch = "src=\"([^\"]+)\"".r findFirstMatchIn this.content
      if (srcMatch.isDefined) {
        val src = srcMatch.get.group(1)
        Seq(src)
      } else {
        Seq()
      }
    } else if (this.name == "image") {
      val srcMatch = "xlink:href=\"([^\"]+)\"".r findFirstMatchIn this.content
      if (srcMatch.isDefined) {
        val src = srcMatch.get.group(1)
        Seq(src)
      } else {
        Seq()
      }
    } else if (this.name == "a") {
        val hrefMatch = "href=\"([^\"]+)\"".r findFirstMatchIn this.content
        if (hrefMatch.isDefined) {
          val href = hrefMatch.get.group(1)
          Seq(href)
        } else {
          Seq()
        }
    } else if (this.name == "tr") {
      val srcPattern = "src=\"([^\"]+)\"".r
      val srcMatches = srcPattern.findAllMatchIn(this.content)
      val hrefPattern = "href=\"([^\"]+)\"".r
      val hrefMatches = hrefPattern.findAllMatchIn(this.content)
      srcMatches.toSeq.map(m => m.group(1)) ++ hrefMatches.toSeq.map(m => m.group(1))
    } else if (children.nonEmpty) {
      children.flatMap(c => c.getResources())
    } else {
      Seq()
    }
  }

  def srcTransform(transformFunction: String => String): Unit = {
    if (this.name == "img") {
      val srcMatch = "src=\"([^\"]+)\"".r findFirstMatchIn this.content
      if (srcMatch.isDefined) {
        val oldSrc = srcMatch.get.group(1)
        val newSrc = transformFunction(oldSrc)
        val oldStart = srcMatch.get.start(1)
        val oldEnd = oldStart + oldSrc.size
        this.content = this.content.substring(0, oldStart) + newSrc + this.content.substring(oldEnd)
      }
    } else if (this.name == "image") {
      val srcMatch = "xlink:href=\"([^\"]+)\"".r findFirstMatchIn this.content
      if (srcMatch.isDefined) {
        val oldSrc = srcMatch.get.group(1)
        val newSrc = transformFunction(oldSrc)
        val oldStart = srcMatch.get.start(1)
        val oldEnd = oldStart + oldSrc.size
        this.content = this.content.substring(0, oldStart) + newSrc + this.content.substring(oldEnd)
      }
    } else if (this.name == "tr") {
      val srcPattern = "src=\"([^\"]+)\"".r
      val matches = srcPattern.findAllMatchIn(this.content)
      var newContent = ""
      var lastMatchEnd = 0
      for (m <- matches.iterator) {
        val oldSrc = m.group(1)
        val newSrc = transformFunction(oldSrc)
        val oldStart = m.start(1)
        val oldEnd = oldStart + oldSrc.size
        newContent += this.content.substring(lastMatchEnd, oldStart) + newSrc
        lastMatchEnd = oldEnd
      }
      this.content = newContent + this.content.substring(lastMatchEnd)
    }
    this.children.foreach(child => child.srcTransform(transformFunction))
  }

  def hrefTransform(transformFunction: String => (String, String)): Unit = {
    if (this.name == "a") {
      val srcMatch = "(href)=\"([^\"]+)\"".r findFirstMatchIn this.content
      if (srcMatch.isDefined) {
        val oldHref = srcMatch.get.group(2)
        val (newName, newHref) = transformFunction(oldHref)
        val oldTagStart = srcMatch.get.start(1)
        val oldStart = srcMatch.get.start(2)
        val oldEnd = oldStart + oldHref.size
        this.content = this.content.substring(0, oldTagStart) + newName + "=\"" + newHref + this.content.substring(oldEnd)
      }
    } else if (this.name == "tr") {
      val srcPattern = "(href)=\"([^\"]+)\"".r
      val matches = srcPattern.findAllMatchIn(this.content)
      var newContent = ""
      var lastMatchEnd = 0
      for (m <- matches.iterator) {
        val oldHref = m.group(2)
        val (newName, newHref) = transformFunction(oldHref)
        val oldTagStart = m.start(1)
        val oldStart = m.start(2)
        val oldEnd = oldStart + oldHref.size
        newContent += this.content.substring(lastMatchEnd, oldTagStart) + newName + "=\"" + newHref
        lastMatchEnd = oldEnd
      }
      this.content = newContent + this.content.substring(lastMatchEnd)
    }
    this.children.foreach(child => child.hrefTransform(transformFunction))
  }

  private def updatePositions(entrancePosition: Long): Unit = {
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

  def nextNode(): BookNode = {
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
        val sibling = parent.children(parent.children.indexOf(current) + 1)
        sibling
      } else {
        // we have reached root, this was the last leaf, there is no other
        null
      }
    } else {
      current.children.head
    }
  }

  def nextNodeOfName(name: String): BookNode = {
    var current = this.nextNode()
    while (current != null) {
      if (current.name == name) return current
      current = current.nextNode()
    }
    null
  }

  def nextLeaf(): BookNode = {
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

  def previousLeaf(): BookNode = {
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

  def leafAtPosition(position: Long): BookNode = {
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

  @JsonIgnore
  def getRoot(): BookNode = {
    var current = this
    while (current.parent != null) current = current.parent
    current
  }
  @JsonIgnore
  def getDocumentStart(): Long = this.getRoot.start
  @JsonIgnore
  def getDocumentEnd(): Long = this.getRoot.end

  def findSpaceAfter(position: Long): Long = {
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
      val m = spacePattern.findFirstMatchIn(leaf.content.substring(searchStartPosition.toInt))
      if (m.isDefined) {
        return m.get.start + position + 1
      }
    }
    if (leaf != null) leaf.end
    else getDocumentEnd()
  }

  def findSpaceBefore(position: Long): Long = {
    val spacePattern = "\\s[^\\s]*$".r
    var leaf = leafAtPosition(position)
    if (leaf != null && leaf.name == "text") {
      val searchText = leaf.content.substring(0, (position - leaf.start).toInt)
      val m = spacePattern.findFirstMatchIn(searchText)
      if (m.isDefined) {
        return m.get.start + leaf.start
      }
    }
    if (leaf != null) {
      leaf = leaf.previousLeaf()
    }
    if (leaf != null) leaf.end
    else getDocumentStart()
  }

  def copy(from: Long, to: Long): BookNode = {
    if (this.name == "text") {
      if (from <= this.start && this.end <= to) {
        // this node is copied whole
        new BookNode("text", this.content, this.start, this.end)
      } else if (from <= this.start && this.start <= to && to<= this.end) {
        // copy ends at this node
        new BookNode(this.name, this.content.substring(0, (to - this.start + 1).toInt), this.start, to)
      } else if (this.start <= from && from <= this.end && this.end <= to) {
        // copy starts at this node
        new BookNode(this.name, this.content.substring((from - this.start).toInt), from, this.end)
      } else if (this.start <= from && to < this.end) {
        // we only copy part of this node
        new BookNode(this.name, this.content.substring((from - this.start).toInt, (to - this.start + 1).toInt), from, to)
      } else {
        null
      }
    } else if (shouldBeLeafElement(this.name)) {
      if (from <= this.start && this.end <= to) {
        // include element in selection
        new BookNode(this.name, this.content, this.start, this.end)
      } else {
        null
      }
    } else {
      if (this.end < from || this.start > to) {
        // this node is outside the range and should not be copied
        null
      } else {
        val newNode = new BookNode(this.name, this.content)
        newNode.children = this.children
          .map(_.copy(from, to))
          .filter(_ != null)
        newNode.children.foreach(_.parent = newNode)
        if (newNode.children.isEmpty) {
          newNode.start = this.start
          newNode.end = this.end
        } else {
          newNode.start = newNode.children.head.start
          newNode.end = newNode.children.last.end
        }
        newNode
      }
    }
  }

  @JsonIgnore
  def getIds(): Map[String, Long] = {
    getId(this.content).map(id => (id -> this.start)).toMap ++ this.children.flatMap(child => child.getIds())
  }
}
