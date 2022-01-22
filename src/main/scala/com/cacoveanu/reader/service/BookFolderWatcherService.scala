package com.cacoveanu.reader.service

import com.cacoveanu.reader.service.BookFolderChangeType.{ADDED, BookFolderChangeType, DELETED, MODIFIED}
import com.cacoveanu.reader.util.FileUtil

import java.io.File
import java.nio.file.{FileSystems, Path, Paths, StandardWatchEventKinds, WatchEvent, WatchKey}
import java.nio.file.StandardWatchEventKinds._
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue, ConcurrentHashMap, ConcurrentLinkedQueue, LinkedBlockingQueue}
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

object BookFolderChangeType extends Enumeration {
  type BookFolderChangeType = Value
  val ADDED, MODIFIED, DELETED = Value
}

case class BookFolderChange(path: String, isFile: Boolean, typ: BookFolderChangeType)

class BookFolderWatcherService(root: String) {

  val watchService = FileSystems.getDefault().newWatchService()
  val watchServiceKeyMap = new ConcurrentHashMap[String, WatchKey]()
  val changesQueue: BlockingQueue[BookFolderChange] = new LinkedBlockingQueue[BookFolderChange]()
  private val handlingEvents: AtomicBoolean = new AtomicBoolean(false)

  def startWatching(path: String): Unit = {
    val key: WatchKey = Paths.get(path).register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    watchServiceKeyMap.put(path, key)
  }

  def stopWatching(path: String) = {
    if (watchServiceKeyMap.containsKey(path)) {
      val key = watchServiceKeyMap.get(path)
      key.cancel()
      watchServiceKeyMap.remove(path)
    }
  }

  def init() = {
    FileUtil.scanFolderTree(root).foreach(f => changesQueue.put(BookFolderChange(f, false, ADDED)))
    //val path = Paths.get(root)
    //val watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)

    // on service initialization
    // scan file/folder structure
    // register folders with the watch service
    // scan all files, including checksums
    // once done, start the watcher reaction service

    // when watchers identify changes on the folder structure, they add those changes to a queue
    // then, the watcher reaction service is triggered, which reads the queue and processes the changes
    // once the watcher reaction service finished running, if it had anything to process in this instance, it starts itself again?

    new Thread(() => {
      var key: WatchKey = null
      while ( {
        key = watchService.take
        key != null
      }) {
        for (event <- key.pollEvents.asScala) {
          //var eventPath = key.watchable().asInstanceOf[Path].resolveSibling(event.context())
          val eventFile = key.watchable().asInstanceOf[Path].resolve(event.context().asInstanceOf[Path]).toFile
          val eventPath = eventFile.getAbsolutePath
          //System.out.println("Event kind:" + event.kind + ". File affected: " + eventPath)
          val typ = event.kind() match {
            case ENTRY_CREATE => ADDED
            case ENTRY_MODIFY => MODIFIED
            case ENTRY_DELETE => DELETED
          }
          changesQueue.put(BookFolderChange(eventPath, eventFile.isFile, typ))
        }
        key.reset
      }
    }).start()

    new Thread(() => {
      while (true) {
        val change = changesQueue.take()
        //println(change)
        change match {
          case BookFolderChange(path, _, DELETED) =>
            println(s"stop following changes in possible folder $path and delete possible book at $path")
            stopWatching(path)
          case BookFolderChange(path, false, ADDED) =>
            println(s"start following changes in folder $path")
            startWatching(path)
          case BookFolderChange(path, true, ADDED) =>
            println(s"scan new book $path")
          //case BookFolderChange(path, false, DELETED) =>
          //  println(s"delete book $path")
          case BookFolderChange(path, true, MODIFIED) =>
            println(s"rescan and update book $path")
          case _ =>
            println("do nothing")
        }
      }
    }).start()
  }

}

object BookFolderWatcherService {
  def main(args: Array[String]): Unit = {
    val root = "D:\\books2" //args(0)
    val service = new BookFolderWatcherService(root)
    service.init()

  }
}