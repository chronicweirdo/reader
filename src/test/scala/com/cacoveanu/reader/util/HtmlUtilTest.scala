package com.cacoveanu.reader.util

import org.junit.jupiter.api.Test
import com.cacoveanu.reader.util.HtmlUtil.AugmentedHtmlString
import org.jsoup.nodes.Document

class HtmlUtilTest {

  private val HTML_STRING =
    """
      |<html>
      |<head>
      |  <title>test html</title>
      |</head>
      |<body>
      |  <h1>html test</h1>
      |  <p>this is just a test html</p>
      |</body>
      |</html>
      |""".stripMargin

  @Test
  def testHtmlConversion(): Unit = {
    val html = HTML_STRING.asHtml

    assert(html != null)
    assert(html.isInstanceOf[Document])
  }
}
