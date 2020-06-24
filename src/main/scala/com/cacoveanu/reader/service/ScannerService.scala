package com.cacoveanu.reader.service

import java.nio.file.{FileSystems, Path, Paths, StandardWatchEventKinds, WatchKey, WatchService}

import com.cacoveanu.reader.util.FileUtil
import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.{Autowired, Value}
import sun.swing.FilePane.FileChooserUIAccessor

import scala.jdk.CollectionConverters._
import scala.beans.BeanProperty
import scala.collection.{immutable, mutable}
import scala.concurrent.{ExecutionContext, Future}

trait FolderChangeListener {
  def handle(created: Seq[String], modified: Seq[String], deleted: Seq[String]): Unit
}

/**
 * This class watches a folder and all its subfolders for changes.
 * When a subfolder is created, changed, or deleted, the watched subfolders are updated accordingly.
 * When files change, are added or deleted, lists of files (full paths) are generated and sent to a listener
 * that know what to do with that information.
 *
 * The service has two threads:
 * - one polls the file system continuously, detects changes and sends information about those changes to a queue
 * - another monitors the changes queue and, after a time threshold, publishes those changes to the listener service
 * (which in this case will be the comic service)
 *
 * This would probably work a lot nicer with actors.
 */
class ScannerService {

  @Value("${comics.location}")
  @BeanProperty
  var rootFolder: String = _

  @BeanProperty
  @Autowired
  var listener: FolderChangeListener = _

  var watchService: WatchService = _
  var watchKeyMap: mutable.Map[WatchKey, String] = new mutable.HashMap[WatchKey, String]()

  var changes = Seq[(String, String)]()
  var lastChange: Long = System.currentTimeMillis()

  val changeThreshold = 4000

  private implicit val executionContext = ExecutionContext.global

  @PostConstruct
  def init(): Unit = {
    val folders = FileUtil.scanFolders(rootFolder)

    watchService = FileSystems.getDefault.newWatchService
    folders.foreach(f => {
      registerPath(Paths.get(f))
    })

    run()
    emit()
  }

  def registerPath(p: Path) = {
    val watchKey = p.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE)
    watchKeyMap.put(watchKey, p.toAbsolutePath.toString)
  }

  def deregisterPath(path: Path) = {
    watchKeyMap.find(e => e._2 == path.toAbsolutePath.toString) match {
      case Some((k, _)) => watchKeyMap.remove(k)
      case None => None
    }
  }

  def run(): Unit = Future {
    while(true) {
      val watchKey = watchService.take()
      if (watchKey != null && watchKeyMap.contains(watchKey)) {
        val source = watchKeyMap(watchKey)
        for (event <- watchKey.pollEvents.asScala) {
          val eventKind = event.kind().name()
          val context = Paths.get(source, event.context().toString)
          if (context.toFile.isDirectory) {
            if (eventKind == StandardWatchEventKinds.ENTRY_CREATE.name()) {
              registerPath(context)
            } else if (eventKind == StandardWatchEventKinds.ENTRY_CREATE.name()) {
              deregisterPath(context)
            }
          }
          this.synchronized {
            changes = changes :+ (eventKind, context.toAbsolutePath.toString)
          }
        }
        lastChange = System.currentTimeMillis()
        watchKey.reset()
      }
    }
  }

  def emit(): Unit = Future {
    while(true) {
      if (System.currentTimeMillis() - lastChange > changeThreshold) {
        val changesBatch = this.synchronized {
          val c = changes
          changes = Seq()
          c
        }
        if (changesBatch.nonEmpty) {
          val created = changesBatch.filter(_._1 == StandardWatchEventKinds.ENTRY_CREATE.name()).map(_._2)
          val modified = changesBatch.filter(_._1 == StandardWatchEventKinds.ENTRY_MODIFY.name()).map(_._2)
          val deleted = changesBatch.filter(_._1 == StandardWatchEventKinds.ENTRY_DELETE.name()).map(_._2)
          listener.handle(created, modified, deleted)
        }
      }
      Thread.sleep(changeThreshold)
    }
  }
}

object ScannerService extends App {
  val service = new ScannerService()
  service.rootFolder = "C:\\Users\\silvi\\Desktop\\New folder"
  service.listener = (c: Seq[String], m: Seq[String], d: Seq[String]) => {
    println(s"created: $c")
    println(s"modified: $m")
    println(s"deleted: $d")
  }
  service.init()
  println("press key to end")
  System.in.read()
}