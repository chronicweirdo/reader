package com.cacoveanu.reader.util

import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.batik.transcoder.{SVGAbstractTranscoder, TranscoderInput, TranscoderOutput}
import org.apache.batik.util.XMLResourceDescriptor
import org.apache.commons.io.output.ByteArrayOutputStream
import org.springframework.core.io.ClassPathResource

import java.io.FileOutputStream

object LogoUtil {

  def generate(logoSvg: String, backgroundColor: String, foregroundColor: String, size: Float): Array[Byte] = {
    val svg = new ClassPathResource(logoSvg).getURL.toString

    val doc = new SAXSVGDocumentFactory(
      XMLResourceDescriptor.getXMLParserClassName())
      .createSVGDocument(svg)

    val r = doc.getElementById("path_r")
    r.setAttribute("style", s"fill:$foregroundColor;fill-opacity:1;stroke:none;")
    val cLeft = doc.getElementById("path_c_left")
    cLeft.setAttribute("style", s"fill:$foregroundColor;fill-opacity:1;stroke:none;")
    val cRight = doc.getElementById("path_c_right")
    cRight.setAttribute("style", s"fill:$foregroundColor;fill-opacity:1;stroke:none;")
    val background = doc.getElementById("background")
    background.setAttribute("style", s"fill:$backgroundColor;fill-opacity:1;stroke:none;")

    val inputSvgImage: TranscoderInput = new TranscoderInput(doc)
    val pngOutputStream = new ByteArrayOutputStream()
    val pngOutputImage = new TranscoderOutput(pngOutputStream)
    val pngTranscoder = new PNGTranscoder();
    pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, size)
    pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, size)
    pngTranscoder.transcode(inputSvgImage, pngOutputImage);
    pngOutputStream.flush()
    pngOutputStream.close()

    pngOutputStream.toByteArray
  }
}
