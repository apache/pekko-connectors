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

package org.apache.pekko.stream.connectors.jms

import javax.jms
import org.apache.pekko.util.FunctionConverters._

/**
 * A destination to send to/receive from.
 */
sealed abstract class Destination {
  val name: String
  val create: jms.Session => jms.Destination
}

object Destination {

  /**
   * Create a [[Destination]] from a [[javax.jms.Destination]]
   */
  def apply(destination: jms.Destination): Destination = destination match {
    case queue: jms.Queue => Queue(queue.getQueueName)
    case topic: jms.Topic => Topic(topic.getTopicName)
    case _                => CustomDestination(destination.toString, _ => destination)
  }

  /**
   * Java API: Create a [[Destination]] from a [[javax.jms.Destination]]
   */
  def createDestination(destination: jms.Destination): Destination = apply(destination)
}

/**
 * Specify a topic as destination to send to/receive from.
 */
final case class Topic(override val name: String) extends Destination {
  override val create: jms.Session => jms.Destination = session => session.createTopic(name)
}

/**
 * Specify a durable topic destination to send to/receive from.
 */
final case class DurableTopic(name: String, subscriberName: String) extends Destination {
  override val create: jms.Session => jms.Destination = session => session.createTopic(name)
}

/**
 * Specify a queue as destination to send to/receive from.
 */
final case class Queue(override val name: String) extends Destination {
  override val create: jms.Session => jms.Destination = session => session.createQueue(name)
}

/**
 * Destination factory to create specific destinations to send to/receive from.
 */
final case class CustomDestination(override val name: String, override val create: jms.Session => jms.Destination)
    extends Destination {

  /** Java API */
  def this(name: String, create: java.util.function.Function[jms.Session, jms.Destination]) =
    this(name, create.asScala)
}
