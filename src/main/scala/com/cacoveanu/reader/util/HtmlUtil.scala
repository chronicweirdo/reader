package com.cacoveanu.reader.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object HtmlUtil {

  def addMeta(html: String, meta: Map[String, String]) = {
    val doc = Jsoup.parse(html)
    val head = doc.select("head").first()

    meta.map(e => {
      val el = new Element("meta")
      el.attr("name", e._1)
      el.attr("content", e._2)
      el
    }).foreach(n => head.appendChild(n))

    doc.toString
  }
}
