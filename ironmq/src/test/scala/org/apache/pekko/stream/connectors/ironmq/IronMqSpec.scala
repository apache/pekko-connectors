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

package org.apache.pekko.stream.connectors.ironmq

import java.util.UUID

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.ironmq.impl.IronMqClient
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.Materializer
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._
import scala.util.hashing.MurmurHash3
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

abstract class IronMqSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterEach
    with LogCapturing {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 15.seconds, interval = 1.second)
  val DefaultActorSystemTerminateTimeout: Duration = 10.seconds

  import ExecutionContext.Implicits.global

  private var mutableIronMqClient = Option.empty[IronMqClient]

  private var mutableConfig = Option.empty[Config]
  def config: Config = mutableConfig.getOrElse(throw new IllegalStateException("Config not initialized"))

  protected def initConfig(): Config =
    ConfigFactory.parseString(s"""pekko.connectors.ironmq {
                                 |  credentials {
                                 |    project-id = "${MurmurHash3.stringHash(System.currentTimeMillis().toString)}"
                                 |  }
                                 |}
      """.stripMargin).withFallback(ConfigFactory.load())

  /**
   * Override to tune the time the test will wait for the actor system to terminate.
   */
  def actorSystemTerminateTimeout: Duration = DefaultActorSystemTerminateTimeout

  private var mutableActorSystem = Option.empty[ActorSystem]
  private var mutableMaterializer = Option.empty[Materializer]

  implicit def actorSystem: ActorSystem =
    mutableActorSystem.getOrElse(throw new IllegalArgumentException("The ActorSystem is not initialized"))
  implicit def materializer: Materializer =
    mutableMaterializer.getOrElse(throw new IllegalStateException("Materializer not initialized"))

  def ironMqClient: IronMqClient =
    mutableIronMqClient.getOrElse(throw new IllegalStateException("The IronMqClient is not initialized"))

  override protected def beforeEach(): Unit = {
    mutableConfig = Option(initConfig())
    mutableActorSystem = Option(ActorSystem(s"test-${System.currentTimeMillis()}", config))
    mutableMaterializer = Option(Materializer(mutableActorSystem.get))
    mutableIronMqClient = Option(IronMqClient(IronMqSettings(config.getConfig("pekko.connectors.ironmq"))))
  }

  override protected def afterEach(): Unit = {
    mutableIronMqClient = Option.empty
    Await.result(actorSystem.terminate(), actorSystemTerminateTimeout)
  }

  def givenQueue(name: String): String =
    ironMqClient.createQueue(name).futureValue

  def givenQueue(): String =
    givenQueue(s"test-${UUID.randomUUID()}")

}
