package com.cacoveanu.reader.util

object BookNode2 {

  private val VOID_ELEMENTS = Seq("area","base","br","col","hr","img","input","link","meta","param","keygen","source")

  // if it starts and ends with angle brackets
  def isTag(str: String) = "^</?[^>]+>$".r matches str
  def isEndTag(str: String) = "^</[^>]+>$".r matches str
  def isBothTag(str: String) = "^<[^>/]+/>$".r matches str
  def tagName(str: String) = "</?([^>\\s]+)".r findFirstMatchIn str match {
    case Some(m) =>
      m.group(1)
    case None => null
  }
  def voidElement(tagName: String) = VOID_ELEMENTS.contains(tagName.toLowerCase())

  def parse(body: String): Option[BookNode2] = {
    val bodyNode = new BookNode2("body", null)
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
          val name = tagName(content)
          // can only be a tag
          if (isEndTag(content)) {
            // we check that this tag closes the current node correctly
            if (voidElement(name)) {
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
          } else if (isBothTag(content) || voidElement(name)) {
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

    Some(bodyNode)
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
}
