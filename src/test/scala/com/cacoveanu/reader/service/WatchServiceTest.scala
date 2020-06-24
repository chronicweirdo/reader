package com.cacoveanu.reader.service

import java.nio.file.{FileSystems, Path, Paths, StandardWatchEventKinds, WatchKey, WatchService}
import java.nio.file.WatchEvent.Kind._
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.nio.file.WatchEvent
import scala.jdk.CollectionConverters._

import scala.concurrent.{ExecutionContext, Future}

class MonitorService(watchService: WatchService) {
  var running = true
  private implicit val executionContext = ExecutionContext.global

  def run(): Unit = {
    Future {
      while(running) {
        val watchKey = watchService.take();
        if (watchKey != null) {
          println("got something")

          for (event <- watchKey.pollEvents.asScala) {

            println(event.kind())
            println(event.context())
          }
          watchKey.reset()
        }
      }
    }
  }
}

object WatchServiceTest {


  // https://docs.oracle.com/javase/tutorial/essential/io/notification.html
  def main(args: Array[String]): Unit = {
    val library = "C:\\Users\\silvi\\Desktop\\New folder"
    val path = Paths.get(library)


    val watchService = FileSystems.getDefault.newWatchService
    val watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

    // no callbacks offered, need to poll the watchkey
    val monitor = new MonitorService(watchService)
    monitor.run()

    println("press key to end")
    System.in.read()
    monitor.running = false
  }

}
