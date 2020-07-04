package com.cacoveanu.reader.service

import java.io.ByteArrayOutputStream
import java.net.{URI, URL, URLEncoder}
import java.nio.file.{LinkOption, Paths}
import java.util.zip.ZipFile

import com.cacoveanu.reader.entity.Book
import com.cacoveanu.reader.repository.BookRepository
import com.cacoveanu.reader.util.{EpubUtil, FileUtil}
import javax.annotation.PostConstruct
import org.apache.tomcat.util.http.fileupload.IOUtils
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

import scala.beans.BeanProperty
import scala.collection.immutable
import scala.jdk.CollectionConverters._
import scala.xml._
import scala.xml.transform._


object EbookService {
  val log: Logger = LoggerFactory.getLogger(classOf[EbookService])

  def main(args: Array[String]): Unit = {
    val service = new EbookService
    //service.booksLocation = "c:\\ws\\reader"
    //service.booksLocation = "d:\\books"

    /*val path = "text/part0004.html"
    val data = service.readFromEpub("Algorithms.epub", path)
    data.foreach(b => {
      val html = new String(b)
      val newHtml = service.processHtml(html, path, "1")
      println(newHtml)
    })*/

    //service.loadToc("1").foreach(println)
    /*service.scanFile("D:\\books\\2020.06 books\\German\\How to Learn German Fast, Including Grammar.epub", "x") match {
      case Some(b) =>
        println(b.title)
        println(b.author)
        println(b.collection)
        println(b.mediaType)
        println(b.cover.length)
      case None => println("scan did not work")
    }*/

    //service.scan().foreach(b => println(b.author + " -> " + b.title))
  }
}

// add <script src="reader.js"></script> to every html header
// <link href="book?id=1&amp;path=stylesheet.css" type="text/css" rel="stylesheet">
class ResourcesAppendRule(resources: Map[String, String]) extends RewriteRule {
  private def getJavascriptNode(link: String) = <script src={link}></script>
  private def getCssNode(link: String) = <link href={link} type="text/css" rel="stylesheet"></link>
  override def transform(n: Node) = n match {
    case elem@Elem(prefix, label, attr, scope, nodes@_*) if label == "head" =>
      val resourceNodes = resources.flatMap(e => e._1 match {
        case "js" => Some(getJavascriptNode(e._2))
        case "css" => Some(getCssNode(e._2))
        case _ => None
      })
      //Elem(prefix, label, attr, scope, true, nodes ++ <script src="/reader.js"></script>: _*)
      Elem(prefix, label, attr, scope, true, nodes ++ resourceNodes: _*)
    case _ => n
  }
}

// <meta name="comicId" content="1">
class MetaAppendRule(metas: Map[String, String]) extends RewriteRule {
  override def transform(n: Node) = n match {
    case elem@Elem(prefix, label, attr, scope, nodes@_*) if label == "head" =>
      val metaNodes: immutable.Iterable[Elem] = metas.map(e => <meta name={e._1} content={e._2}></meta>)
      //metaNodes.foreach(n => println(">>> " + n.toString()))
      val newHeaderNodes: Seq[Node] = nodes ++ metaNodes
      newHeaderNodes.foreach(n => println(">>> " + n.toString()))
      Elem(prefix, label, attr, scope, true, newHeaderNodes : _*)
    case _ => n
  }
}

case class TocEntry(index: Int, title: String, link: String)

case class Resource(mimeType: String, content: Array[Byte], prev: Option[String], next: Option[String])

class LinkRewriteRule(bookId: String, htmlPath: String) extends RewriteRule {

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
    //println(">>> " + remotePath)
    val remoteUri = new URI(remotePath)
    if (remoteUri.isAbsolute) {
      remotePath
    } else {
      val (externalPath, internalPath) = splitPath(remotePath)
      val remotePathWithFolder = if (folder.length > 0) folder + "/" + externalPath else externalPath
      val normalizedPath = Paths.get(remotePathWithFolder).normalize().toString.replaceAll("\\\\", "/")
      s"book?id=$bookId&path=${URLEncoder.encode(normalizedPath, "UTF-8")}" + (if (internalPath != null) "#" + internalPath else "")
      //s"book?id=$bookId&path=$normalizedPath"
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

@Service
class EbookService {

  import EbookService.log

 /* @Value("${library.location}")
  @BeanProperty
  var booksLocation: String = _*/

  /*@BeanProperty
  @Autowired
  var bookRepository: BookRepository = _*/

  /*@PostConstruct
  def updateLibrary() = scan()*/

  /*private val BOOK_TYPE_EPUB = "epub"
  private val BOOK_FILE_REGEX = ".+\\.(" + BOOK_TYPE_EPUB + ")$"
  private val CONTENT_REGEX = ".+\\.opf$"*/

  private def readEpub(path: String) = {
    var zipFile: ZipFile = null

    try {
      zipFile = new ZipFile(path)
      zipFile.entries().asScala.foreach(println)
      val ncx: Option[String] = zipFile.entries().asScala.find(e => e.getName.endsWith(".ncx")).map(f => {
        val fileContents = zipFile.getInputStream(f)
        val bos = new ByteArrayOutputStream()
        IOUtils.copy(fileContents, bos)
        new String(bos.toByteArray)
      })
      //ncx.foreach(println)
      ncx match {
        case Some(n) =>
          (XML.loadString(n) \\ "navPoint").foreach(println)
        case None =>
      }
    } catch {
      case e: Throwable =>
        e.printStackTrace()
    } finally {
      zipFile.close()
    }
  }

  /*private val hrefRule = new RewriteRule {
    override def transform(n: Node) = n match {
      case e @ <a>{_*}</a> => e.asInstanceOf[Elem] %
        Attribute(null, "attr1", "200",
          Attribute(null, "attr2", "100", Null))
      case _ => n
    }
  }*/

  private def processHtml(html: String, path: String, bookId: String): String = {
    // make XML
    val xml: Elem = XML.loadString(html)
    // adjust all links
    val xml2: collection.Seq[Node] = new RuleTransformer(new LinkRewriteRule(bookId, path)).transform(xml)
    val xml3: collection.Seq[Node] = new RuleTransformer(new ResourcesAppendRule(Map("js" -> "/reader.js", "css" -> "/reader.css"))).transform(xml2)

    val toc = loadToc(bookId)
    val metas: Map[String, String] = Map(
      "nextSection" -> getNext(toc, path).map(e => e.link),
      "prevSection" -> getPrev(toc, path).map(e => e.link),
      "bookId" -> Some(bookId)
    ).filter(e => e._2.isDefined).map(e => (e._1, e._2.getOrElse("")))
    println(metas)
    val xml4 = new RuleTransformer(new MetaAppendRule(metas)).transform(xml3)

    //println(xml3.size)
    xml4.head.toString()
  }

  /*private def readFromEpub(epubPath: String, resourcePath: String): Option[Array[Byte]] = {
    var zipFile: ZipFile = null

    try {
      zipFile = new ZipFile(epubPath)
      zipFile.entries().asScala.find(e => e.getName == resourcePath).map(f => {
        val fileContents = zipFile.getInputStream(f)
        val bos = new ByteArrayOutputStream()
        IOUtils.copy(fileContents, bos)
        bos.toByteArray
      })
    } catch {
      case e: Throwable =>
        log.error(s"failed to read epub $epubPath", e)
        None
    } finally {
      zipFile.close()
    }
  }*/

  /*private def findInEpub(epubPath: String, resourceRegex: String): Option[(String, Array[Byte])] = {
    var zipFile: ZipFile = null
    val pattern = resourceRegex.r

    try {
      zipFile = new ZipFile(epubPath)
      zipFile.entries().asScala.find(e => pattern.pattern.matcher(e.getName.toLowerCase).matches).map(f => {
        val fileContents = zipFile.getInputStream(f)
        val bos = new ByteArrayOutputStream()
        IOUtils.copy(fileContents, bos)
        (f.getName, bos.toByteArray)
      })
    } catch {
      case e: Throwable =>
        log.error(s"failed to read epub $epubPath", e)
        None
    } finally {
      zipFile.close()
    }
  }*/

  private def getFiletype(path: String) = {
    val dot = path.lastIndexOf(".")
    val extension = path.substring(dot + 1).toLowerCase()
    extension match {
      case "html" => MediaType.TEXT_HTML_VALUE
      case "jpg" => MediaType.IMAGE_JPEG_VALUE
      case "jpeg" => MediaType.IMAGE_JPEG_VALUE
      case "gif" => MediaType.IMAGE_GIF_VALUE
      case "png" => MediaType.IMAGE_PNG_VALUE
      case "css" => "text/css"
    }
  }

  private def parseToc(xmlString: String) = {
    val xml: Elem = XML.loadString(xmlString)
    (xml \\ "navPoint").map(n => TocEntry(
      (n \\ "@playOrder").text.toInt,
      (n \\ "text").text,
      (n \\ "@src").text
    ))
  }

  def loadToc(bookId: String) = {
    EpubUtil.readResource("Algorithms.epub", "toc.ncx") match {
      case Some(bytes) => parseToc(new String(bytes, "UTF-8"))
      case None => Seq()
    }
  }

  private def getBaseResourcePath(resourcePath: String) = {
    val r = if (resourcePath.indexOf('#') > 0) resourcePath.substring(0, resourcePath.indexOf('#'))
    else resourcePath
    r
  }

  private def getPrev(toc: Seq[TocEntry], resourcePath: String) = {
    val baseResourcePath = getBaseResourcePath(resourcePath)

    toc.zipWithIndex.find(e => getBaseResourcePath(e._1.link) == baseResourcePath).map(_._2) match {
      case Some(index) => toc.zipWithIndex.find(e => e._2 == index - 1).map(_._1)
      case None => None
    }
  }

  private def getNext(toc: Seq[TocEntry], resourcePath: String) = {
    val baseResourcePath = getBaseResourcePath(resourcePath)

    toc.zipWithIndex.find(e => getBaseResourcePath(e._1.link) == baseResourcePath).map(_._2) match {
      case Some(index) => toc.zipWithIndex.find(e => e._2 == index + 1).map(_._1)
      case None => None
    }
  }

  def loadResource(bookId: String, resourcePath: String): Option[(String, Array[Byte])] = {
    EpubUtil.readResource("Algorithms.epub", resourcePath) match {
      case Some(bytes) =>
        getFiletype(resourcePath) match {
          case "text/html" =>
            val data: Array[Byte] = processHtml(new String(bytes, "UTF-8"), resourcePath, bookId)
              .getBytes("UTF-8")
            Some(("text/html", data))
          case "text/css" => Some(("text/css", bytes))
          case "image/jpeg" => Some(("image/jpeg", bytes))
          case "image/png" => Some(("image/png", bytes))
          case "image/gif" => Some(("image/gif", bytes))
          case _ => None
        }
      case None => None
    }
  }

  /*private def getTitle(contentOpf: Elem): Option[String] =
    (contentOpf \ "metadata" \ "title").headOption.map(_.text)

  private def getAuthor(contentOpf: Elem): Option[String] =
    (contentOpf \ "metadata" \ "creator").headOption.map(_.text)*/

  /*private def getCollection(path: String): Option[String] = {
    val pathObject = Paths.get(path);
    val collectionPath = Paths.get(booksLocation).relativize(pathObject.getParent)
    Some(collectionPath.toString)
  }*/

  /*private def getCoverResource(contentOpf: Elem): Option[(String, String)] = {
    (contentOpf \ "metadata" \ "meta")
      .find(n => (n \ "@name").text == "cover")
      .map(n => (n \ "@content").text)
      .flatMap(id => {
        (contentOpf \\ "manifest" \ "item")
          .find(node => (node \ "@id").text == id )
          .map(node => ((node \ "@href").text, (node \ "@media-type").text))
      })
  }

  private def getXml(data: Array[Byte]) = {
    try {
      Some(XML.loadString(new String(data, "UTF-8")))
    } catch {
      case _: Throwable => None
    }
  }*/

  private def computeRelativePath(pov: String, resource: String) = {
    if (resource.startsWith("/") || pov.lastIndexOf("/") < 0) {
      resource
    } else {
      val root = pov.substring(0, pov.lastIndexOf("/"))
      root + "/" + resource
    }
  }

  /*private def scanFile(path: String, checksum: String): Option[DbBook] = {
    EpubUfindInEpub(path, CONTENT_REGEX).flatMap(e => getXml(e._2).map(xml => (e._1, xml))) match {
      case Some((opfPath, contentOpf)) =>
        //val contentOpf: Elem = XML.loadString(new String(contentBytes, "UTF-8"))

        val cover: Option[(Array[Byte], String)] = getCoverResource(contentOpf).flatMap { case (resource, contentType) => readFromEpub(path, computeRelativePath(opfPath, resource)) match {
          case Some(bytes) => Some((bytes, contentType))
          case None => None
        }}
        (
          getTitle(contentOpf),
          getAuthor(contentOpf),
          getCollection(path),
          cover
        ) match {
          case (Some(title), Some(author), Some(collection), Some((cover, contentType))) =>
            // id: String, path: String, title: String, author: String, collection: String, mediaType: String, cover: Array[Byte]
            val size = 0
            Some(new DbBook(checksum, path, title, author, collection, contentType, cover, size))
          case (None, _, _, _) =>
            println(s"missing title for $path")
            None
          case (_, None, _, _) =>
            println(s"missing author for $path")
            None
          case (_, _, None, _) =>
            println(s"missing collection for $path")
            None
          case (_, _, _, None) =>
            println(s"missing cover for $path")
            None
          case _ => None
        }
      case None => None
    }
  }*/


}
