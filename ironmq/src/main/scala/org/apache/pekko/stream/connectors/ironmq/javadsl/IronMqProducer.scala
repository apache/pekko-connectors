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

package org.apache.pekko.stream.connectors.ironmq.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.japi.Pair
import pekko.{ Done, NotUsed }
import pekko.stream.connectors.ironmq._
import pekko.stream.javadsl.{ Flow, Sink }
import pekko.stream.scaladsl.{ Flow => ScalaFlow, Keep }
import pekko.stream.connectors.ironmq.scaladsl.{ IronMqProducer => ScalaIronMqProducer }
import pekko.util.FutureConverters

object IronMqProducer {

  import FutureConverters._

  def flow(queueName: String, settings: IronMqSettings): Flow[PushMessage, String, NotUsed] =
    ScalaIronMqProducer
      .flow(queueName, settings)
      .asJava
      // To make Message.Id a String
      .asInstanceOf[Flow[PushMessage, String, NotUsed]]

  def sink(queueName: String, settings: IronMqSettings): Sink[PushMessage, CompletionStage[Done]] =
    ScalaIronMqProducer.sink(queueName, settings).mapMaterializedValue(_.asJava).asJava

  def atLeastOnceFlow[C1 <: Committable](
      queueName: String,
      settings: IronMqSettings): Flow[CommittablePushMessage[C1], String, NotUsed] =
    ScalaFlow[CommittablePushMessage[C1]]
      .map { cm =>
        cm.message -> cm.toCommit.asScala
      }
      .via(ScalaIronMqProducer.atLeastOnceFlow(queueName, settings))
      .asJava
      // To make Message.Id a String
      .asInstanceOf[Flow[CommittablePushMessage[C1], String, NotUsed]]

  def atLeastOnceSink[C1 <: Committable](queueName: String,
      settings: IronMqSettings): Sink[CommittablePushMessage[C1], NotUsed] =
    ScalaFlow[CommittablePushMessage[C1]]
      .map { cm =>
        cm.message -> cm.toCommit.asScala
      }
      .to(ScalaIronMqProducer.atLeastOnceSink(queueName, settings))
      .asJava

  def atLeastOnceFlow[ToCommit, CommitResult, CommitMat](
      queueName: String,
      settings: IronMqSettings,
      commitFlow: Flow[ToCommit, CommitResult, CommitMat])
      : Flow[CommittablePushMessage[ToCommit], Pair[String, CommitResult], CommitMat] =
    ScalaFlow[CommittablePushMessage[ToCommit]]
      .map { cm =>
        cm.message -> cm.toCommit
      }
      .viaMat(ScalaIronMqProducer.atLeastOnceFlow(queueName, settings, commitFlow.asScala))(
        Keep.right)
      .map(p => Pair(p._1.asInstanceOf[String], p._2))
      .asJava

  def atLeastOnceSink[ToCommit, CommitResult, CommitMat](
      queueName: String,
      settings: IronMqSettings,
      commitFlow: Flow[ToCommit, CommitResult, CommitMat]): Sink[CommittablePushMessage[ToCommit], CommitMat] =
    ScalaFlow[CommittablePushMessage[ToCommit]]
      .map { cm =>
        cm.message -> cm.toCommit
      }
      .toMat(ScalaIronMqProducer.atLeastOnceSink(queueName, settings, commitFlow.asScala))(
        Keep.right)
      .asJava

}

case class CommittablePushMessage[ToCommit](message: PushMessage, toCommit: ToCommit)

object CommittablePushMessage {

  def create[ToCommit](message: PushMessage, toCommit: ToCommit): CommittablePushMessage[ToCommit] =
    CommittablePushMessage(message, toCommit)

  def create[ToCommit](messageBody: String, toCommit: ToCommit): CommittablePushMessage[ToCommit] =
    CommittablePushMessage(PushMessage(messageBody), toCommit)

  def create(committableMessage: CommittableMessage): CommittablePushMessage[CommittableMessage] =
    CommittablePushMessage(PushMessage(committableMessage.message.body), committableMessage)
}
