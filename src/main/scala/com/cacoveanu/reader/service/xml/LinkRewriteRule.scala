package com.cacoveanu.reader.service.xml

import java.net.{URI, URLEncoder}
import java.nio.file.Paths

import scala.xml.{Attribute, Elem, Node, Null, Unparsed}
import scala.xml.transform.RewriteRule

class LinkRewriteRule(bookId: String, htmlPath: String) extends RewriteRule {

  private val BASE_LINK = "bookResource"

  private def getFolder(path: String) = {
    val lio = path.lastIndexOf("/")
    if (lio > 0) path.substring(0, lio)
    else ""
  }

  private val folder = getFolder(htmlPath)

  private def splitPath(path: String) = {
    val di = path.lastIndexOf("#")
    if (di > 0) (path.substring(0, di), path.substring(di+1))
    else (path, null)
  }

  private def getNewLink(remotePath: String): String = {
    val remoteUri = new URI(remotePath)
    if (remoteUri.isAbsolute) {
      remotePath
    } else {
      val (externalPath, internalPath) = splitPath(remotePath)
      val remotePathWithFolder = if (folder.length > 0) folder + "/" + externalPath else externalPath
      val normalizedPath = Paths.get(remotePathWithFolder).normalize().toString.replaceAll("\\\\", "/")
      s"$BASE_LINK?id=$bookId&path=${URLEncoder.encode(normalizedPath, "UTF-8")}" + (if (internalPath != null) "#" + internalPath else "")
    }
  }

  private def extractHrefString(elem: Elem) = elem.attribute("href").map(c => c.head).map(n => n.toString()).getOrElse("")

  private def extractSrcString(elem: Elem) = elem.attribute("src").map(c => c.head).map(n => n.toString()).getOrElse("")

  private def transformElementWithHref(e: Node): Node = {
    val original: Elem = e.asInstanceOf[Elem]
    val originalHref = extractHrefString(original)
    val newHref = getNewLink(originalHref)
    original % Attribute("href", Unparsed(newHref), Null)
  }

  private def transformElementWithSrc(e: Node): Node = {
    val original: Elem = e.asInstanceOf[Elem]
    val originalSrc = extractSrcString(original)
    val newSrc = getNewLink(originalSrc)
    original % Attribute("src", Unparsed(newSrc), Null)
  }

  override def transform(n: Node) = n match {
    case e @ <a>{_*}</a> => transformElementWithHref(e)
    case e @ <link>{_*}</link> => transformElementWithHref(e)
    case e @ <img>{_*}</img> => transformElementWithSrc(e)
    case _ => n
  }
}
