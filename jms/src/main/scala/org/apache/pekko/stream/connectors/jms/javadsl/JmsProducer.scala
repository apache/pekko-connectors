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

package org.apache.pekko.stream.connectors.jms.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.stream.connectors.jms.{ scaladsl, JmsEnvelope, JmsMessage, JmsProducerSettings }
import pekko.stream.javadsl.Source
import pekko.stream.scaladsl.{ Flow, Keep }
import pekko.util.ByteString
import pekko.{ Done, NotUsed }

import scala.jdk.CollectionConverters._
import scala.compat.java8.FutureConverters

/**
 * Factory methods to create JMS producers.
 */
object JmsProducer {

  /**
   * Create a flow to send [[pekko.stream.connectors.jms.JmsMessage JmsMessage]] sub-classes to
   * a JMS broker.
   */
  def flow[R <: JmsMessage](
      settings: JmsProducerSettings): pekko.stream.javadsl.Flow[R, R, JmsProducerStatus] =
    pekko.stream.connectors.jms.scaladsl.JmsProducer.flow(settings).mapMaterializedValue(
      toProducerStatus).asJava

  /**
   * Create a flow to send [[pekko.stream.connectors.jms.JmsEnvelope JmsEnvelope]] sub-classes to
   * a JMS broker to support pass-through of data.
   */
  def flexiFlow[PassThrough](
      settings: JmsProducerSettings)
      : pekko.stream.javadsl.Flow[JmsEnvelope[PassThrough], JmsEnvelope[PassThrough], JmsProducerStatus] =
    pekko.stream.connectors.jms.scaladsl.JmsProducer
      .flexiFlow[PassThrough](settings)
      .mapMaterializedValue(toProducerStatus)
      .asJava

  /**
   * Create a sink to send [[pekko.stream.connectors.jms.JmsMessage JmsMessage]] sub-classes to
   * a JMS broker.
   */
  def sink[R <: JmsMessage](
      settings: JmsProducerSettings): pekko.stream.javadsl.Sink[R, CompletionStage[Done]] =
    pekko.stream.connectors.jms.scaladsl.JmsProducer
      .sink(settings)
      .mapMaterializedValue(FutureConverters.toJava)
      .asJava

  /**
   * Create a sink to send Strings as text messages to a JMS broker.
   */
  def textSink(settings: JmsProducerSettings): pekko.stream.javadsl.Sink[String, CompletionStage[Done]] =
    pekko.stream.connectors.jms.scaladsl.JmsProducer
      .textSink(settings)
      .mapMaterializedValue(FutureConverters.toJava)
      .asJava

  /**
   * Create a sink to send byte arrays to a JMS broker.
   */
  def bytesSink(
      settings: JmsProducerSettings): pekko.stream.javadsl.Sink[Array[Byte], CompletionStage[Done]] =
    pekko.stream.connectors.jms.scaladsl.JmsProducer
      .bytesSink(settings)
      .mapMaterializedValue(FutureConverters.toJava)
      .asJava

  /**
   * Create a sink to send [[pekko.util.ByteString ByteString]]s to a JMS broker.
   */
  def byteStringSink(
      settings: JmsProducerSettings): pekko.stream.javadsl.Sink[ByteString, CompletionStage[Done]] =
    pekko.stream.connectors.jms.scaladsl.JmsProducer
      .byteStringSink(settings)
      .mapMaterializedValue(FutureConverters.toJava)
      .asJava

  /**
   * Create a sink to send map structures to a JMS broker.
   */
  def mapSink(
      settings: JmsProducerSettings): pekko.stream.javadsl.Sink[java.util.Map[String, Any], CompletionStage[Done]] = {

    val scalaSink =
      pekko.stream.connectors.jms.scaladsl.JmsProducer
        .mapSink(settings)
        .mapMaterializedValue(FutureConverters.toJava)
    val javaToScalaConversion =
      Flow.fromFunction((javaMap: java.util.Map[String, Any]) => javaMap.asScala.toMap)
    javaToScalaConversion.toMat(scalaSink)(Keep.right).asJava
  }

  /**
   * Create a sink to send serialized objects to a JMS broker.
   */
  def objectSink(
      settings: JmsProducerSettings): pekko.stream.javadsl.Sink[java.io.Serializable, CompletionStage[Done]] =
    pekko.stream.connectors.jms.scaladsl.JmsProducer
      .objectSink(settings)
      .mapMaterializedValue(FutureConverters.toJava)
      .asJava

  private def toProducerStatus(scalaStatus: scaladsl.JmsProducerStatus) = new JmsProducerStatus {

    override def connectorState: Source[JmsConnectorState, NotUsed] =
      scalaStatus.connectorState.map(_.asJava).asJava
  }
}
