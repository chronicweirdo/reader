package com.cacoveanu.reader.service.xml

import scala.xml.{Elem, Node}
import scala.xml.transform.RewriteRule

class ResourceAppendRule(resources: Map[String, String]) extends RewriteRule {
  private def getJavascriptNode(link: String) = <script src={link}></script>
  private def getCssNode(link: String) = <link href={link} type="text/css" rel="stylesheet"></link>
  override def transform(n: Node) = n match {
    case elem@Elem(prefix, label, attr, scope, nodes@_*) if label == "head" =>
      val resourceNodes = resources.flatMap(e => e._1 match {
        case "js" => Some(getJavascriptNode(e._2))
        case "css" => Some(getCssNode(e._2))
        case _ => None
      })
      Elem(prefix, label, attr, scope, true, nodes ++ resourceNodes: _*)
    case _ => n
  }
}
