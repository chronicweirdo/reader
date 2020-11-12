package com.cacoveanu.reader

import java.nio.charset.StandardCharsets

import com.cacoveanu.reader.util.EpubUtil

import scala.collection.mutable
import scala.util.matching.Regex

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
        if (from <= this.start && this.end <= to) {
          // this node is copied whole
          val nn = new BookNode()
          nn.typ = this.typ
          nn.content = this.content
          nn.start = this.start
          nn.end = this.end
          nn
        } else if (from <= this.start && this.start <= to && to <= this.end) {
          // copy ends at this node
          val nn = new BookNode()
          nn.typ = this.typ
          nn.content = this.content.substring(0, to - this.start + 1)
          nn.start = this.start
          nn.end = to
          nn
        } else if (this.start <= from && from <= this.end && this.end <= to) {
          // copy starts at this node
          val nn = new BookNode()
          nn.typ = this.typ
          nn.content = this.content.substring(from - this.start)
          nn.start = from
          nn.end = this.end
          nn
        } else if (this.start <= from && to <= this.end) {
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
          nn.start = nn.children.head.start
          nn.end = nn.children.last.end
          nn
        }

    }
  }
}

object TestParseHtml {

  def getHtmlBody(html: String) = {
    val bodyStartPattern = "<body[^>]*>".r
    val bodyStartMatch: Option[Regex.Match] = bodyStartPattern findFirstMatchIn html
    //println(bodyStartMatch)
    //println(bodyStartMatch.get.start)
    //println(bodyStartMatch.get.end)

    val bodyEndPattern = "</body\\s*>".r
    val bodyEndMatch = bodyEndPattern findFirstMatchIn html

    (bodyStartMatch, bodyEndMatch) match {
      case (Some(sm), Some(em)) => Some(html.substring(sm.end, em.start))
      case _ => None
    }
  }

  // returns a tuple
  // the first entry in the tuple is true if it's a beginning tag, false if it's an end tag
  // the second entry in the tuple is the tag name
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

  case class Node(start: Int, length: Int, content: String, typ: String, tagStack: Seq[(String, String)])

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
            newImageNode.end = position + 1
            position = newImageNode.end
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
              currentNode.end = currentNode.start
            }
            currentNode = currentNode.parent
          case None if currentContent.length > 0 =>
            // a text node ended
            val newTextNode = new BookNode("text", currentNode)
            newTextNode.content = currentContent
            newTextNode.start = position
            newTextNode.end = position + currentContent.length
            position = newTextNode.end
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

  def parseHtmlBody(body: String) = {

    var inTag = false
    var currentTag = ""
    var currentText = ""
    var startPosition = 0
    var position = 0
    var tagStack = mutable.Stack[(String, String)]()
    var nodes = Seq[Node]()
    for (i <- 0 until body.length) {
      if (inTag) {
        currentTag += body(i)
        if (body(i) == '>') {
          inTag = false
          println("collected tag " + currentTag)
          parseTag(currentTag) match {
            case Some((true, "img")) =>
              println("found an image")
              position += 1
              nodes = nodes :+ Node(startPosition, position - startPosition, currentTag, "img", tagStack.toSeq)
              startPosition = position
            case Some((true, name)) => tagStack.push((name, currentTag))
            case Some((false, name)) =>
              val (poppedName, _) = tagStack.pop()
              if (poppedName != name) throw new Throwable("popped tag does not match end tag found")
            case _ => throw new Throwable("failed to parse tag")
          }
          currentTag = ""
          startPosition = position
        }
      } else {
        if (body(i) == '<') {
          inTag = true
          println("collected text " + currentText)
          println("at position " + position)
          println("under tag stack" + tagStack.map(_._1))
          if (currentText.length > 0) {
            nodes = nodes :+ Node(startPosition, position - startPosition, currentText, "text", tagStack.toSeq)
            currentText = ""
          }
          currentTag += body(i)
        } else {
          currentText += body(i)
          position += 1
        }
      }
    }
    nodes
  }

  // collecting from nodes this way if pretty hard
  // this is buggy
  // and still needs to handle the tag stack
  def getFromNodes(nodes: Seq[Node], from: Int, to: Int): String = {
    var collecting = false
    var text = ""
    for (i <- nodes.indices) {
      if (!collecting && from >= nodes(i).start && from < nodes(i).start + nodes(i).length) {
        if (to > nodes(i).start + nodes(i).length) {
          collecting = true
          if (nodes(i).typ == "text") {
            text += nodes(i).content.substring(from - nodes(i).start)
          } else {
            text += nodes(i).content
          }
        } else {
          // collecting only from this node
          if (nodes(i).typ == "text") {
            text += nodes(i).content.substring(from - nodes(i).start, to - nodes(i).start)
          } else {
            text += nodes(i).content
          }
          return text
        }
      }
      if (collecting) {
        if (to > nodes(i).start + nodes(i).length) {
          // collect this node and continue
          text += nodes(i).content
        } else {
          // stop collecting at this node
          if (nodes(i).typ == "text") {
            text += nodes(i).content.substring(0, to - nodes(i).start)
          } else {
            text += nodes(i).content
          }
          return text
        }
      }
    }
    text
  }

  def main(args: Array[String]): Unit = {
    val path = "C:\\Users\\silvi\\Desktop\\test\\book1.epub"
    val toc = EpubUtil.getToc(path)
    val sections = EpubUtil.getSections(toc)
    val section = sections(7)
    println(section.link)
    val bytes = EpubUtil.readResource(path, section.link).get
    val html = new String(bytes, StandardCharsets.UTF_8)
    println(html)
    val body = getHtmlBody(html)
    println(body)
    if (body.isDefined) {
      /*val nodes = parseHtmlBody(body.get)
      nodes.foreach(println)
      println()
      val part = getFromNodes(nodes, 100, 1100)
      println(part)*/
      val bodyText = body.get
      val bodyNode = parseHtmlBodyToTree(bodyText)
      //println(bodyNode)
      bodyNode.prettyPrint()
      val backToContent = bodyNode.getContent()
      println(bodyText == backToContent)
      println(bodyText.length)
      println(backToContent.length)
      //println(backToContent)

      val subtree = bodyNode.copy(0, 99)
      subtree.prettyPrint()
      println(subtree.getContent())
      println()
      val subtree2 = bodyNode.copy(100, 199)
      subtree2.prettyPrint()
      println(subtree2.getContent())
      val subtree3 = bodyNode.copy(200, 299)
      subtree3.prettyPrint()
      println(subtree3.getContent())
    }

  }
}
