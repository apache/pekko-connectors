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

package org.apache.pekko.stream.connectors.ironmq.impl
import java.util.UUID

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.connectors.ironmq.IronMqSettings
import org.apache.pekko.stream.Materializer

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.hashing.MurmurHash3

trait IronMqClientForTests {

  implicit def system: ActorSystem = ActorSystem()
  implicit def materializer: Materializer = Materializer(system)
  implicit lazy val executionContext: ExecutionContext = system.dispatcher

  val projectId = s"""${MurmurHash3.stringHash(System.currentTimeMillis().toString)}"""

  val ironMqSettings: IronMqSettings = IronMqSettings(system.settings.config.getConfig(IronMqSettings.ConfigPath))
    .withProjectId(projectId)

  val ironMqClient = IronMqClient(ironMqSettings)

  def givenQueue(name: String): Future[String] =
    ironMqClient.createQueue(name)

  def givenQueue(): Future[String] =
    givenQueue(s"test-${UUID.randomUUID()}")

}

class IronMqClientForJava(_system: ActorSystem, _mat: Materializer) extends IronMqClientForTests {
  override implicit def system: ActorSystem = _system
  override implicit def materializer: Materializer = _mat

}
