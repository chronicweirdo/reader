package com.cacoveanu.reader

import java.nio.charset.StandardCharsets

import com.cacoveanu.reader.util.EpubUtil

import scala.collection.mutable
import scala.util.matching.Regex

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
  }

  case class Node(start: Int, length: Int, content: String, typ: String, tagStack: Seq[(String, String)])

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
      val nodes = parseHtmlBody(body.get)
      nodes.foreach(println)
      println()
      val part = getFromNodes(nodes, 100, 1100)
      println(part)
    }

  }
}
