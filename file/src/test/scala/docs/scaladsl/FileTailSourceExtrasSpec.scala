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

import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.connectors.file.DirectoryChange
import org.apache.pekko.stream.connectors.file.scaladsl.{ DirectoryChangesSource, FileTailSource }
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.stream.scaladsl.{ Keep, Source }
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.apache.pekko.testkit.TestKit
import com.google.common.jimfs.{ Configuration, Jimfs }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class FileTailSourceExtrasSpec
    extends TestKit(ActorSystem("filetailsourceextrasspec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with LogCapturing {

  private val fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform.toBuilder.build)

  "The FileTailSource" should assertAllStagesStopped {
    "demo stream shutdown when file deleted" in {
      val path = fs.getPath("/file")
      Files.write(path, "a\n".getBytes(UTF_8))

      // #shutdown-on-delete

      val checkInterval = 1.second
      val fileCheckSource = DirectoryChangesSource(path.getParent, checkInterval, 8192)
        .collect {
          case (p, DirectoryChange.Deletion) if path == p =>
            throw new FileNotFoundException(path.toString)
        }
        .recoverWithRetries(1,
          {
            case _: FileNotFoundException => Source.empty
          })

      val stream = FileTailSource
        .lines(path = path, maxLineSize = 8192, pollingInterval = 250.millis)
        .merge(fileCheckSource, eagerComplete = true)

      // #shutdown-on-delete

      val probe = stream.toMat(TestSink.probe)(Keep.right).run()

      val result = probe.requestNext()
      result shouldEqual "a"

      Files.delete(path)

      probe.request(1)
      probe.expectComplete()
    }

    "demo stream shutdown when with idle timeout" in {
      val path = fs.getPath("/file")
      Files.write(path, "a\n".getBytes(UTF_8))

      // #shutdown-on-idle-timeout

      val stream = FileTailSource
        .lines(path = path, maxLineSize = 8192, pollingInterval = 250.millis)
        .idleTimeout(5.seconds)
        .recoverWithRetries(1,
          {
            case _: TimeoutException => Source.empty
          })

      // #shutdown-on-idle-timeout

      val probe = stream.toMat(TestSink.probe)(Keep.right).run()

      val result = probe.requestNext()
      result shouldEqual "a"

      Thread.sleep(5.seconds.toMillis + 1000)

      probe.expectComplete()
    }

  }
}
