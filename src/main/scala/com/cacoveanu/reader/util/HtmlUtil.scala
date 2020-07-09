package com.cacoveanu.reader.util

import java.net.{URI, URLEncoder}
import java.nio.file.Paths

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

object HtmlUtil {

  implicit class AugmentedHtmlString(text: String) {
    def asHtml: Document = Jsoup.parse(text)
  }

  implicit class AugmentedJsoupDocument(doc: Document) {
    def addResources(resources: Seq[(String, String)]): Document = appendResources(doc, resources)
    def addMeta(meta: Map[String, String]): Document = appendMeta(doc, meta)
    def transformLinks(contextPath: String, bookId: String): Document = transformLinksInternal(doc, contextPath, bookId)
    def asString: String = doc.toString
  }

  private val BASE_LINK = "bookResource"

  private def appendResources(doc: Document, resources: Seq[(String, String)]) = {
    val head = doc.select("head").first()

    resources.map(e => e._1 match {
      case "js" =>
        val el = new Element("script")
        el.attr("src", e._2)
        el
      case "css" =>
        val el = new Element("link")
        el.attr("type", "text/css")
        el.attr("rel", "stylesheet")
        el.attr("href", e._2)
        el
    }).foreach(n => head.appendChild(n))

    doc
  }

  private def appendMeta(doc: Document, meta: Map[String, String]) = {
    val head = doc.select("head").first()

    meta.map(e => {
      val el = new Element("meta")
      el.attr("name", e._1)
      el.attr("content", e._2)
      el
    }).foreach(n => head.appendChild(n))

    doc
  }

  private def transformLinksInternal(doc: Document, contextPath: String, bookId: String) = {
    doc.select("a").forEach(a => a.attr("href", transformLink(a.attr("href"), contextPath, bookId)))
    doc.select("link").forEach(a => a.attr("href", transformLink(a.attr("href"), contextPath, bookId)))
    doc.select("img").forEach(a => a.attr("src", transformLink(a.attr("src"), contextPath, bookId)))

    doc
  }

  private def transformLink(originalLink: String, contextPath: String, bookId: String): String = {
    val remoteUri = new URI(originalLink)
    if (remoteUri.isAbsolute) {
      originalLink
    } else {
      val (externalPath, internalPath) = splitPath(originalLink)
      val folder = getFolder(contextPath)
      val remotePathWithFolder = if (folder.length > 0) folder + "/" + externalPath else externalPath
      val normalizedPath = Paths.get(remotePathWithFolder).normalize().toString.replaceAll("\\\\", "/")
      s"$BASE_LINK?id=$bookId&path=${URLEncoder.encode(normalizedPath, "UTF-8")}" + (if (internalPath != null) "#" + internalPath else "")
    }
  }

  private def getFolder(path: String) = {
    val lio = path.lastIndexOf("/")
    if (lio > 0) path.substring(0, lio)
    else ""
  }

  private def splitPath(path: String) = {
    val di = path.lastIndexOf("#")
    if (di > 0) (path.substring(0, di), path.substring(di+1))
    else (path, null)
  }
}
