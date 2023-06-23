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

package org.apache.pekko.stream.connectors.mqtt.streaming.impl

import org.apache.pekko
import pekko.actor.typed.scaladsl.{ ActorContext, Behaviors }
import pekko.actor.typed.{ Behavior, Signal }

import scala.collection.immutable.Seq

object BehaviorRunner {
  sealed trait Interpretable[T]

  final case class StoredMessage[T](message: T) extends Interpretable[T]

  final case class StoredSignal[T](signal: Signal) extends Interpretable[T]

  /**
   * Interpreter all of the supplied messages or signals, returning
   * the resulting behavior.
   */
  def run[T](behavior: Behavior[T], context: ActorContext[T], stash: Seq[Interpretable[T]]): Behavior[T] =
    stash.foldLeft(Behavior.start(behavior, context)) {
      case (b, StoredMessage(msg)) =>
        val nextBehavior = Behavior.interpretMessage(b, context, msg)

        if ((nextBehavior ne Behaviors.same) && (nextBehavior ne Behaviors.unhandled)) {
          nextBehavior
        } else {
          b
        }

      case (b, StoredSignal(signal)) =>
        val nextBehavior = Behavior.interpretSignal(b, context, signal)

        if ((nextBehavior ne Behaviors.same) && (nextBehavior ne Behaviors.unhandled)) {
          nextBehavior
        } else {
          b
        }
    }
}
