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

package org.apache.pekko.stream.connectors.s3.impl.auth

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.s3.impl.SplitAfterSizeWithContext
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Flow, Sink, Source }
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.testkit.TestKit
import pekko.util.ByteString
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class SplitAfterSizeWithContextSpec(_system: ActorSystem)
    extends TestKit(_system)
    with AnyFlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with LogCapturing {

  def this() = this(ActorSystem("SplitAfterSizeSpec"))
  implicit val defaultPatience: PatienceConfig = PatienceConfig(1.seconds, 5.millis)

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  final val MaxChunkSize = 1024
  "SplitAfterSize" should "yield a single empty substream on no input" in assertAllStagesStopped {
    Source
      .empty[(ByteString, Int)]
      .via(
        SplitAfterSizeWithContext(10)(Flow[(ByteString, Int)]).concatSubstreams)
      .runWith(Sink.seq)
      .futureValue should be(Seq.empty)
  }

  it should "start a new stream after the element that makes it reach a maximum, but not split the element itself" in assertAllStagesStopped {
    Source(Vector((ByteString(1, 2, 3, 4, 5), 1), (ByteString(6, 7, 8, 9, 10, 11, 12), 2), (ByteString(13, 14), 3)))
      .via(
        SplitAfterSizeWithContext(10)(Flow[(ByteString, Int)])
          .prefixAndTail(10)
          .map { case (prefix, tail) => prefix }
          .concatSubstreams)
      .runWith(Sink.seq)
      .futureValue should be(
      Seq(
        Seq((ByteString(1, 2, 3, 4, 5), 1), (ByteString(6, 7, 8, 9, 10, 11, 12), 2)),
        Seq((ByteString(13, 14), 3))))
  }

  it should "not split large elements (unlike SplitAfterSize)" in assertAllStagesStopped {
    Source(Vector((ByteString(bytes(1, 16)), 1), (ByteString(17, 18), 2)))
      .via(
        SplitAfterSizeWithContext(10)(Flow[(ByteString, Int)])
          .prefixAndTail(10)
          .map { case (prefix, tail) => prefix }
          .concatSubstreams)
      .runWith(Sink.seq)
      .futureValue should be(
      Seq(
        Seq((ByteString(bytes(1, 16)), 1)),
        Seq((ByteString(17, 18), 2))))
  }

  def bytes(start: Byte, end: Byte): Array[Byte] = (start to end).map(_.toByte).toArray[Byte]

}
