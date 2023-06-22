/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.scaladsl

import java.nio.file.FileSystems

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.stream.scaladsl.Source

import scala.concurrent.duration._

object FileTailSourceSpec {

  // small sample of usage, tails the first argument file path
  def main(args: Array[String]): Unit = {
    if (args.length != 1) throw new IllegalArgumentException("Usage: FileTailSourceTest [path]")
    val path: String = args(0)

    implicit val system: ActorSystem = ActorSystem()

    // #simple-lines
    import org.apache.pekko.stream.connectors.file.scaladsl.FileTailSource

    val fs = FileSystems.getDefault
    val lines: Source[String, NotUsed] = FileTailSource.lines(
      path = fs.getPath(path),
      maxLineSize = 8192,
      pollingInterval = 250.millis)

    lines.runForeach(line => System.out.println(line))
    // #simple-lines
  }

}
