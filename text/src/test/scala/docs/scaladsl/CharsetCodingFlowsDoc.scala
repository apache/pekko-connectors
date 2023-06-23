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

import java.nio.file.Paths

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.IOResult
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.testkit.TestKit
import pekko.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{ BeforeAndAfterAll, RecoverMethods }

import scala.collection.immutable
import scala.concurrent.Future

class CharsetCodingFlowsDoc
    extends TestKit(ActorSystem("charset"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with RecoverMethods
    with LogCapturing {

  val multiByteChars = "äåû經濟商行政管理总局التجارى"

  "Encoding" should {

    "be illustrated in a documentation example" in {
      // format: off
      // #encoding
      import java.nio.charset.StandardCharsets

      import org.apache.pekko
      import pekko.stream.connectors.text.scaladsl.TextFlow
      import pekko.stream.scaladsl.FileIO
      import pekko.util.ccompat.JavaConverters._

      // #encoding
      val targetFile = Paths.get("target/outdata.txt")
      val strings = System.getProperties.asScala.map(p => p._1 + " -> " + p._2).toList
      // #encoding
      val stringSource: Source[String, _] = // ...
        // #encoding
        Source(strings)
      val result =
        // #encoding

      stringSource
        .via(TextFlow.encoding(StandardCharsets.US_ASCII))
        .intersperse(ByteString("\n"))
        .runWith(FileIO.toPath(targetFile))
      // #encoding
      result.futureValue.count should be > 50L
      // format: on
    }
  }

  "Decoding" should {
    "be illustrated in a documentation example" in {
      // format: off
      // #decoding
      import java.nio.charset.StandardCharsets

      import org.apache.pekko.stream.connectors.text.scaladsl.TextFlow

      // #decoding
      val utf16bytes = ByteString("äåûßêëé", StandardCharsets.UTF_16)
      // #decoding
      val byteStringSource: Source[ByteString, _] = // ...
        // #decoding
        Source
          .single(utf16bytes)
      // #decoding

      val result: Future[immutable.Seq[String]] =
        byteStringSource
          .via(TextFlow.decoding(StandardCharsets.UTF_16))
          .runWith(Sink.seq)
      // #decoding
      result.futureValue should be(Seq("äåûßêëé"))
      // format: on
    }

  }

  "Transcoding" should {
    "be illustrated in a documentation example" in {
      // format: off
      // #transcoding
      import java.nio.charset.StandardCharsets

      import org.apache.pekko
      import pekko.stream.connectors.text.scaladsl.TextFlow
      import pekko.stream.scaladsl.FileIO

      // #transcoding
      val utf16bytes = ByteString("äåûßêëé", StandardCharsets.UTF_16)
      val targetFile = Paths.get("target/outdata-transcoding.txt")
      // #transcoding
      val byteStringSource: Source[ByteString, _] = // ...
      // #transcoding
        Source
          .single(utf16bytes)
      val result: Future[IOResult] =
      // #transcoding

      byteStringSource
        .via(TextFlow.transcoding(StandardCharsets.UTF_16, StandardCharsets.UTF_8))
        .runWith(FileIO.toPath(targetFile))
      // #transcoding
      result.futureValue.count should be > 5L
      // format: on
    }

  }

}
