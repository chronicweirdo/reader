package com.cacoveanu.reader.entity

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.cacoveanu.reader.entity.CollectionNode.EVERYTHING

object CollectionNode {
  val EVERYTHING = "everything"
}

class CollectionNode() {
  var name: String = _
  var search: String = _
  var children: Seq[CollectionNode] = Seq()

  def this(name: String, search: String) {
    this()
    this.name = name
    this.search = URLEncoder.encode(search, StandardCharsets.UTF_8)
  }

  private def getHref(): String = if (search.isEmpty) "/" else "/?search=" + search


  def toHtml(): String = {
    var html = s"""<a href="${getHref()}">${name}</a>"""
    if (children.nonEmpty) {
      html += (if (name == EVERYTHING) """<ul class="tree">""" else "<ul>")
      html += children.map(c => "<li>" + c.toHtml() + "</li>").mkString("") + "</ul>"
    }
    html
  }
}
