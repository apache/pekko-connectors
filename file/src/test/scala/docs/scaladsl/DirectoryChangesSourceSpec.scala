/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.scaladsl
import java.nio.file.FileSystems

import akka.actor.ActorSystem

import scala.concurrent.duration._

object DirectoryChangesSourceSpec {
  def main(args: Array[String]): Unit = {
    if (args.length != 1) throw new IllegalArgumentException("Usage: DirectoryChangesSourceTest [path]")
    val path: String = args(0)

    implicit val system: ActorSystem = ActorSystem()

    // #minimal-sample
    import akka.stream.alpakka.file.scaladsl.DirectoryChangesSource

    val fs = FileSystems.getDefault
    val changes = DirectoryChangesSource(fs.getPath(path), pollInterval = 1.second, maxBufferSize = 1000)
    changes.runForeach {
      case (path, change) => println("Path: " + path + ", Change: " + change)
    }
    // #minimal-sample
  }
}
