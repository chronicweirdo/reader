package com.cacoveanu.reader.entity

object TocNode {
  val ROOT = "ROOT"

  def getTocTree(toc: Seq[BookTocEntry]) = {
    val root = new TocNode(TocNode.ROOT, -1, -1)
    var nodeReferences = Seq[TocNode]()

    for (i <- toc.indices) {
      val current: TocNode = new TocNode(toc(i).title, toc(i).position, toc(i).level)
      nodeReferences = nodeReferences :+ current

      // look for possible parent
      if (toc(i).level > 0) {
        var j = i - 1
        var found = false
        while (j > 0 && !found) {
          if (toc(j).level == toc(i).level - 1) {
            nodeReferences(j).add(current)
            found = true
          }
          j = j - 1
        }
        if (!found) {
          root.add(current)
        }
      } else {
        // this is a root
        root.add(current)
      }
    }
    root
  }
}

class TocNode {
  var title: String = _
  var position: Long = _
  var level: Int = _
  var children: Seq[TocNode] = Seq()

  def this(title: String, position: Long, level: Int) {
    this()
    this.title = title
    this.position = position
    this.level = level
  }

  def add(child: TocNode): Unit = {
    this.children = this.children :+ child
  }

  private def getA(): String = {
    s"""<p><a class="ch_chapter" ch_position="${this.position}" onclick="displayPageForTocEntry(this)">${this.title}</a></p>"""
  }

  private def getLi(): String = {
    if (children.nonEmpty) {
      s"""<li class="ch_withsubchapters">"""
    } else {
      "<li>"
    }
  }

  def toHtml(): String = {
    var html = if (this.title != TocNode.ROOT) getA() else ""
    if (children.nonEmpty) {
      html += (if (this.title != TocNode.ROOT) s"""<ul class="ch_subchapter">""" else "<ul>")
      html += children.map(c => c.getLi() + c.toHtml() + "</li>").mkString("") + "</ul>"
    }
    html
  }
}