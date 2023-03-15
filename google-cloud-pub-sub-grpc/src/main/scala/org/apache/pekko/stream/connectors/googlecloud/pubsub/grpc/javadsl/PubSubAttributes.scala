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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.javadsl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.Attributes
import pekko.stream.Attributes.Attribute

/**
 * Akka Stream attributes that are used when materializing PubSub stream blueprints.
 */
object PubSubAttributes {

  /**
   * gRPC publisher to use for the stream
   */
  def publisher(publisher: GrpcPublisher): Attributes = Attributes(new Publisher(publisher))

  final class Publisher @InternalApi private[PubSubAttributes] (val publisher: GrpcPublisher) extends Attribute

  /**
   * gRPC subscriber to use for the stream
   */
  def subscriber(subscriber: GrpcSubscriber): Attributes = Attributes(new Subscriber(subscriber))

  final class Subscriber @InternalApi private[PubSubAttributes] (val subscriber: GrpcSubscriber) extends Attribute
}
