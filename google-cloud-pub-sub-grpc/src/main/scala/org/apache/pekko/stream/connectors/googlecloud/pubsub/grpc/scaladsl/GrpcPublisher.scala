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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.scaladsl

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
import com.google.pubsub.v1.pubsub.{ PublisherClient => ScalaPublisherClient }

/**
 * Holds the gRPC scala publisher client instance.
 */
final class GrpcPublisher private (settings: PubSubSettings, googleSettings: GoogleSettings, sys: ActorSystem) {

  @ApiMayChange
  final val client =
    ScalaPublisherClient(PekkoGrpcSettings.fromPubSubSettings(settings, googleSettings)(sys))(sys)

  sys.registerOnTermination(client.close())
}

object GrpcPublisher {
  def apply(settings: PubSubSettings,
      googleSettings: GoogleSettings)(implicit sys: ClassicActorSystemProvider): GrpcPublisher =
    new GrpcPublisher(settings, googleSettings, sys.classicSystem)

  def apply(settings: PubSubSettings, googleSettings: GoogleSettings, sys: ActorSystem): GrpcPublisher =
    new GrpcPublisher(settings, googleSettings, sys)

  def apply(settings: PubSubSettings)(implicit sys: ClassicActorSystemProvider): GrpcPublisher =
    new GrpcPublisher(settings, GoogleSettings(), sys.classicSystem)

  def apply(settings: PubSubSettings, sys: ActorSystem): GrpcPublisher =
    new GrpcPublisher(settings, GoogleSettings(sys), sys)

  def apply()(implicit sys: ClassicActorSystemProvider): GrpcPublisher =
    apply(PubSubSettings(sys))

  def apply(sys: ActorSystem): GrpcPublisher =
    apply(PubSubSettings(sys), sys)
}

/**
 * An extension that manages a single gRPC scala publisher client per actor system.
 */
final class GrpcPublisherExt private (sys: ExtendedActorSystem) extends Extension {
  implicit val publisher: GrpcPublisher = GrpcPublisher(sys: ActorSystem)
}

object GrpcPublisherExt extends ExtensionId[GrpcPublisherExt] with ExtensionIdProvider {
  override def lookup = GrpcPublisherExt
  override def createExtension(system: ExtendedActorSystem) = new GrpcPublisherExt(system)

  /**
   * Access to extension from the new and classic actors API.
   */
  def apply()(implicit system: ClassicActorSystemProvider): GrpcPublisherExt = super.apply(system)

  /**
   * Access to the extension from the classic actors API.
   */
  override def apply(system: pekko.actor.ActorSystem): GrpcPublisherExt = super.apply(system)

  /**
   * Java API
   *
   * Access to extension.
   */
  override def get(system: ActorSystem): GrpcPublisherExt = super.get(system)

  /**
   * Java API
   *
   * Access to extension.
   */
  override def get(system: ClassicActorSystemProvider): GrpcPublisherExt = super.get(system)
}
