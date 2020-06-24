package com.cacoveanu.reader.service

import java.nio.file.{FileSystems, Path, Paths, StandardWatchEventKinds, WatchKey, WatchService}

import com.cacoveanu.reader.util.FileUtil
import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import sun.swing.FilePane.FileChooserUIAccessor

import scala.jdk.CollectionConverters._
import scala.beans.BeanProperty
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * This class watches a folder and all its subfolders for changes.
 * When a subfolder is created, changed, or deleted, the watched subfolders are updated accordingly.
 * When files change, are added or deleted, lists of files (full paths) are generated and sent to a listener
 * that know what to do with that information
 */
class ScannerService {

  @Value("${comics.location}")
  @BeanProperty
  var rootFolder: String = _

  var watchService: WatchService = _
  var watchKeyMap: mutable.Map[WatchKey, String] = new mutable.HashMap[WatchKey, String]()

  val changes = new java.util.concurrent.ConcurrentHashMap[(String, String), Unit]()
  var lastChange: Long = System.currentTimeMillis()

  val changeThreshold = 4000

  private implicit val executionContext = ExecutionContext.global

  @PostConstruct
  def init(): Unit = {
    // get all folders and subfolders of the root folder
    val folders = FileUtil.scanFolders(rootFolder)
    println(folders)

    watchService = FileSystems.getDefault.newWatchService
    folders.foreach(f => {
      registerPath(Paths.get(f))//.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE)
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
      //println(watchKey)
      //println(watchKeyMap(watchKey))
      if (watchKey != null && watchKeyMap.contains(watchKey)) {
        val source = watchKeyMap(watchKey)
        //println(s"got something for $source")
        for (event <- watchKey.pollEvents.asScala) {
          val eventName = event.kind().name()
          //println(eventName)
          val context = event.context().toString
          val fullContext = Paths.get(source, context)//.toAbsolutePath.toString
          if (fullContext.toFile.isDirectory) {
            if (eventName == "ENTRY_CREATE") {
              // add watcher on the new folder
              registerPath(fullContext)
            } else if (eventName == "ENTRY_DELETE") {
              deregisterPath(fullContext)
            }
          }
          //println(fullContext)
          changes.put((eventName, fullContext.toAbsolutePath.toString), ())
        }
        lastChange = System.currentTimeMillis()
        watchKey.reset()
        println()
      }
    }
  }

  def emit(): Unit = Future {
    while(true) {
      if (System.currentTimeMillis() - lastChange > changeThreshold) {
        val ch = this.synchronized {
          // emit changes
          val newChanges = changes.keys().asScala.toSeq
          changes.clear()
          newChanges
        }
        if (ch.nonEmpty) {
          println(ch)
        }
      }
      Thread.sleep(1000)
    }
  }
}

object ScannerService extends App {
  val service = new ScannerService()
  service.rootFolder = "C:\\Users\\silvi\\Desktop\\New folder"
  service.init()
  println("press key to end")
  System.in.read()
}