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


    // find all files in path
    // compute new, existing, deleted files and add them to the changes queue




  }

}

object BookFolderWatcherService {
  def main(args: Array[String]): Unit = {
    val root = "D:\\books2" //args(0)
    val service = new BookFolderWatcherService(root)
    service.init()

  }
}