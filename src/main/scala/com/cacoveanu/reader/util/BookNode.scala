package com.cacoveanu.reader.util

import scala.util.matching.Regex

object BookNode {
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

  def parseTag(tagString: String) = {
    val tagNamePattern = "[^>\\s]+".r
    if (tagString.length > 2) {
      if (tagString(0) == '<' && tagString.takeRight(1) == ">") {
        if (tagString(1) == '/') {
          // and end tag
          tagNamePattern.findFirstIn(tagString.substring(2))
            .map(name => (false, name))
        } else {
          // a beginning tag
          tagNamePattern.findFirstIn(tagString.substring(1))
            .map(name => (true, name))
        }
      } else None
    } else None
  }

  def parseHtmlBodyToTree(body: String) = {
    val bodyNode = new BookNode("body", null)
    var currentNode: BookNode = bodyNode

    var currentContent = ""
    var position = 0

    for (i <- 0 until body.length) {
      if (body(i) == '<' || body(i) == '>' || i == body.length - 1) {
        if (body(i) == '>' || i == body.length - 1) currentContent += body(i)
        // starting a new tag, need to save current content to a node
        parseTag(currentContent) match {
          case Some((true, "img")) =>
            val newImageNode = new BookNode("img", currentNode)
            newImageNode.content = currentContent
            newImageNode.start = position
            newImageNode.end = position// + 1
            position = newImageNode.end + 1
            currentNode.children = currentNode.children :+ newImageNode
          case Some((true, tagName)) =>
            // opening a new tag
            val newTagNode = new BookNode(tagName, currentNode)
            newTagNode.content = currentContent
            newTagNode.start = position
            currentNode.children = currentNode.children :+ newTagNode
            currentNode = newTagNode
          case Some((false, tagName)) =>
            // closing the current tag
            if (tagName != currentNode.typ) throw new Throwable("incompatible closing tag")
            if (currentNode.children.nonEmpty) {
              currentNode.end = currentNode.children.last.end
            } else {
              // an empty tag is usually used to include text separation
              // so it should have a length of at least one to allow us to execut pagination before or after it
              currentNode.end = currentNode.start
              position = currentNode.end + 1
            }
            currentNode = currentNode.parent
          case None if currentContent.length > 0 =>
            // a text node ended
            val newTextNode = new BookNode("text", currentNode)
            newTextNode.content = currentContent
            newTextNode.start = position
            newTextNode.end = position + currentContent.length - 1
            position = newTextNode.end + 1
            currentNode.children = currentNode.children :+ newTextNode
          case None =>
          // some empty, probably a tag ended and a new one begins
        }
        currentContent = ""
      }
      if (body(i) != '>') currentContent += body(i)
    }
    if (bodyNode.children.nonEmpty) {
      bodyNode.end = bodyNode.children.last.end
    } else {
      bodyNode.end = bodyNode.start
    }
    bodyNode
  }

  def parseHtmlTree(html: String): Option[BookNode] = {
    getHtmlBody(html).map(body => parseHtmlBodyToTree(body))
  }
}

class BookNode {

  var typ: String = _
  var parent: BookNode = _
  var children: Seq[BookNode] = Seq()
  var content: String = _
  var start: Int = _
  var end: Int = _

  def this(typ: String, parent: BookNode) {
    this()
    this.typ = typ
    this.parent = parent
  }

  override def toString: String =
    "(" + this.typ + "," + this.children.map(_.toString).mkString(",") + ")"

  def prettyPrint(level: Int = 0): Unit = {
    printAtLevel(level, this.typ + "[" + this.start + "," + this.end + "]: " + this.content)
    this.children.foreach(c => c.prettyPrint(level+1))
  }

  def printAtLevel(level: Int, text: String) = {
    for (i <- 0 until level) print("\t")
    println(text)
  }

  def getContent(): String = {
    this.typ match {
      case "text" => this.content
      case "img" => this.content
      case "body" => this.children.map(_.getContent()).mkString("")
      case tagName => this.content + this.children.map(_.getContent()).mkString("") + "</" + tagName + ">"
    }
  }

  /*def inorder(from: Int, to: Int) = {
    if (from <= this.start && this.end <= to) {
      // gets printed fully
      println(this.getContent())
    } else {
    }
  }*/
  def copy(from: Int, to: Int): BookNode = {
    this.typ match {
      case "img" =>
        if (from <= this.start && this.end <= to) {
          // include image in selection
          val nn = new BookNode()
          nn.typ = this.typ
          nn.content = this.content
          nn.start = this.start
          nn.end = this.end
          nn
        } else {
          null
        }
      case "text" =>
        if (from <= this.start && this.end < to) {
          // this node is copied whole
          val nn = new BookNode()
          nn.typ = this.typ
          nn.content = this.content
          nn.start = this.start
          nn.end = this.end
          nn
        } else if (from <= this.start && this.start < to && to < this.end) {
          // copy ends at this node
          val nn = new BookNode()
          nn.typ = this.typ
          nn.content = this.content.substring(0, to - this.start + 1)
          nn.start = this.start
          nn.end = to
          nn
        } else if (this.start <= from && from < this.end && this.end < to) {
          // copy starts at this node
          val nn = new BookNode()
          nn.typ = this.typ
          nn.content = this.content.substring(from - this.start)
          nn.start = from
          nn.end = this.end
          nn
        } else if (this.start <= from && to < this.end) {
          // we only copy part of this node
          val nn = new BookNode()
          nn.typ = this.typ
          nn.content = this.content.substring(from - this.start, to - this.start + 1)
          nn.start = from
          nn.end = to
          nn
        } else {
          null
        }
      case _ =>
        // this is not a leaf
        if (this.end < from || this.start > to) {
          // this node is outside the range and should not be copied
          null
        } else {
          val nn = new BookNode()
          nn.typ = this.typ
          nn.content = this.content
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

  def leafAtPosition(position: Int): BookNode = {
    if (position < this.start || this.end <= position) {
      return null
    } else {
      var currentNode = this
      while (currentNode != null && currentNode.children.nonEmpty) {
        currentNode.children.find(c => c.start <= position && position < c.end) match {
          case Some(nextNode) => currentNode = nextNode
          case None => currentNode = null
        }
      }
      currentNode
    }
  }

  def nextLeaf(): BookNode = {
    if (this.children.nonEmpty) {
      // leaf is a child of this node
      var current = this
      while (current.children.nonEmpty) {
        current = current.children.head
      }
      current
    } else {
      // go up the parent line until we find next siblings, and then find the leaf of a next sibling
      var current = this
      var parent = current.parent
      while (parent != null && parent.children.indexOf(current) == parent.children.size - 1) {
        current = parent
        parent = current.parent
      }
      if (parent != null) {
        val leafCandidate = parent.children(parent.children.indexOf(current) + 1)
        if (leafCandidate.children.isEmpty) leafCandidate
        else leafCandidate.nextLeaf()
      } else null
    }
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
  def root(): BookNode = {
    var current = this
    while (current.parent != null) current = current.parent
    current
  }

  def documentStart(): Int = this.root.start
  def documentEnd(): Int = this.root.end
  def length(): Int = this.end - this.start

  def findSpaceAfter(position: Int): Int = {
    val spacePattern = "\\s".r
    // first get leaf at position
    val leaf = leafAtPosition(position)
    // for a text node, next space may be in the text node, next space character after position
    // if other kind of node, next space is the start of next leaf
    if (leaf != null) {
      leaf.typ match {
        case "text" =>
          spacePattern.findFirstMatchIn(leaf.content.substring(position - leaf.start + 1)) match {
            case Some(m) => m.start + position + 1
            case None =>
              val nextLeaf = leaf.nextLeaf()
              if (nextLeaf != null) nextLeaf.start
              else leaf.documentEnd()
          }
        case _ =>
          val nextLeaf = leaf.nextLeaf()
          if (nextLeaf != null) nextLeaf.start
          else leaf.documentEnd()
      }
    } else this.documentEnd()
  }

  def findSpaceBefore(position: Int): Int = {
    val spacePattern = "\\s[^\\s]*$".r
    val leaf = leafAtPosition(position)
    if (leaf != null) {
      leaf.typ match {
        case "text" =>
          spacePattern.findFirstMatchIn(leaf.content.substring(0, position - leaf.start)) match {
            case Some(m) => m.start + leaf.start
            case None =>
              val previousLeaf = leaf.previousLeaf()
              if (previousLeaf != null) previousLeaf.start
              else leaf.documentStart()
          }
        case _ =>
          // beginning of previous leaf
          val previousLeaf = leaf.previousLeaf()
          if (previousLeaf != null) previousLeaf.start
          else leaf.documentStart()
      }
    } else this.documentStart()
  }
}
