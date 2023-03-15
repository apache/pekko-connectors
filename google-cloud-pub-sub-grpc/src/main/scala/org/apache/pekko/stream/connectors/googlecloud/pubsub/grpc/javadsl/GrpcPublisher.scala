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
import pekko.actor.{
  ActorSystem,
  ClassicActorSystemProvider,
  ExtendedActorSystem,
  Extension,
  ExtensionId,
  ExtensionIdProvider
}
import pekko.annotation.ApiMayChange
import pekko.stream.connectors.google.GoogleSettings
import pekko.stream.connectors.googlecloud.pubsub.grpc.PubSubSettings
import pekko.stream.connectors.googlecloud.pubsub.grpc.impl.PekkoGrpcSettings
import com.google.pubsub.v1.{ PublisherClient => JavaPublisherClient }

/**
 * Holds the gRPC java publisher client instance.
 */
final class GrpcPublisher private (settings: PubSubSettings, googleSettings: GoogleSettings, sys: ActorSystem) {

  @ApiMayChange
  final val client =
    JavaPublisherClient.create(PekkoGrpcSettings.fromPubSubSettings(settings, googleSettings)(sys), sys)

  sys.registerOnTermination(client.close())
}

object GrpcPublisher {

  /**
   * Creates a publisher with the new actors API.
   */
  def create(settings: PubSubSettings, googleSettings: GoogleSettings, sys: ClassicActorSystemProvider): GrpcPublisher =
    create(settings, googleSettings, sys.classicSystem)

  def create(settings: PubSubSettings, googleSettings: GoogleSettings, sys: ActorSystem): GrpcPublisher =
    new GrpcPublisher(settings, googleSettings, sys)

  /**
   * Creates a publisher with the new actors API.
   */
  def create(settings: PubSubSettings, sys: ClassicActorSystemProvider): GrpcPublisher =
    create(settings, sys.classicSystem)

  def create(settings: PubSubSettings, sys: ActorSystem): GrpcPublisher =
    new GrpcPublisher(settings, GoogleSettings(sys), sys)

  /**
   * Creates a publisher with the new actors API.
   */
  def create(sys: ClassicActorSystemProvider): GrpcPublisher =
    create(sys.classicSystem)

  def create(sys: ActorSystem): GrpcPublisher =
    create(PubSubSettings(sys), sys)
}

/**
 * An extension that manages a single gRPC java publisher client per actor system.
 */
final class GrpcPublisherExt private (sys: ExtendedActorSystem) extends Extension {
  implicit val publisher = GrpcPublisher.create(sys)
}

object GrpcPublisherExt extends ExtensionId[GrpcPublisherExt] with ExtensionIdProvider {
  override def lookup = GrpcPublisherExt
  override def createExtension(system: ExtendedActorSystem) = new GrpcPublisherExt(system)

  /**
   * Java API
   *
   * Access to extension.
   */
  override def get(system: ActorSystem): GrpcPublisherExt = super.get(system)

  /**
   * Java API
   *
   * Access to the extension from the new actors API.
   */
  override def get(system: ClassicActorSystemProvider): GrpcPublisherExt = super.get(system)
}
