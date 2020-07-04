package com.cacoveanu.reader.service.xml

import scala.xml.{Elem, Node}
import scala.xml.transform.RewriteRule

/**
 * Append metadata to an HTML XML
 *
 * @param metas - metadata map to append
 */
class MetaAppendRule(metas: Map[String, String]) extends RewriteRule {
  private val metaNodes = metas.map(e => <meta name={e._1} content={e._2}></meta>)

  override def transform(n: Node) = n match {
    case _@Elem(prefix, label, attr, scope, nodes@_*) if label == "head" =>
      Elem(prefix, label, attr, scope, true, nodes ++ metaNodes : _*)
    case _ => n
  }
}
