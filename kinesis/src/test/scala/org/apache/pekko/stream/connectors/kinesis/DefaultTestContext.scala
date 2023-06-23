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

package org.apache.pekko.stream.connectors.kinesis

import java.util.concurrent.{ Executors, TimeoutException }

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, Suite }

import scala.concurrent.duration._
import scala.concurrent.{ blocking, Await, ExecutionContext, ExecutionContextExecutor }

trait DefaultTestContext extends BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar { this: Suite =>

  implicit protected val system: ActorSystem = ActorSystem(
    "KinesisTests",
    ConfigFactory.parseString("""
    pekko.stream.materializer.initial-input-buffer-size = 1
    pekko.stream.materializer.max-input-buffer-size = 1
  """))
  private val threadPool = Executors.newFixedThreadPool(10)
  implicit protected val executionContext: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(threadPool)

  override protected def afterAll(): Unit = {
    Await.ready(system.terminate(), 5.seconds)
    threadPool.shutdown()
    if (!blocking(threadPool.awaitTermination(5, SECONDS)))
      throw new TimeoutException()
  }

}
