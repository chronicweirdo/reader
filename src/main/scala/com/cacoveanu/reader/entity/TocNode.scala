package com.cacoveanu.reader.entity

import java.util
import scala.beans.BeanProperty

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

  @BeanProperty
  var title: String = _

  @BeanProperty
  var position: Long = _

  @BeanProperty
  var level: Int = _

  @BeanProperty
  var children: java.util.List[TocNode] = new util.ArrayList[TocNode]()

  def this(title: String, position: Long, level: Int) = {
    this()
    this.title = title
    this.position = position
    this.level = level
  }

  def add(child: TocNode): Unit = {
    this.children.add(child)
  }
}