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

package org.apache.pekko.stream.connectors.google.util

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.scaladsl.Source
import pekko.stream.testkit.scaladsl.TestSink
import pekko.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AnnotateLastSpec
    extends TestKit(ActorSystem("AnnotateLastSpec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "AnnotateLast" should {

    "indicate last element" in {
      val probe = Source(1 to 3).via(AnnotateLast[Int]).runWith(TestSink.probe)
      probe.requestNext(NotLast(1))
      probe.requestNext(NotLast(2))
      probe.requestNext(Last(3))
      probe.expectComplete()
    }

    "indicate first element is last if only one element" in {
      val probe = Source.single(1).via(AnnotateLast[Int]).runWith(TestSink.probe)
      probe.requestNext(Last(1))
      probe.expectComplete()
    }

    "do nothing when stream is empty" in {
      val probe = Source.empty[Nothing].via(AnnotateLast[Nothing]).runWith(TestSink.probe)
      probe.expectSubscriptionAndComplete()
    }
  }

}
