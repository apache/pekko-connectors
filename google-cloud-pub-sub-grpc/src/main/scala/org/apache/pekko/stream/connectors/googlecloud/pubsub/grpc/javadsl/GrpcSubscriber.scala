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

import org.apache.pekko.actor.{
  ActorSystem,
  ClassicActorSystemProvider,
  ExtendedActorSystem,
  Extension,
  ExtensionId,
  ExtensionIdProvider
}
import org.apache.pekko.annotation.ApiMayChange
import org.apache.pekko.stream.connectors.google.GoogleSettings
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.PubSubSettings
import org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.impl.PekkoGrpcSettings
import com.google.pubsub.v1.{ SubscriberClient => JavaSubscriberClient }

/**
 * Holds the gRPC java subscriber client instance.
 */
final class GrpcSubscriber private (settings: PubSubSettings, googleSettings: GoogleSettings, sys: ActorSystem) {

  @ApiMayChange
  final val client =
    JavaSubscriberClient.create(PekkoGrpcSettings.fromPubSubSettings(settings, googleSettings)(sys), sys)

  sys.registerOnTermination(client.close())
}

object GrpcSubscriber {

  /**
   * Creates a publisher with the new actors API.
   */
  def create(settings: PubSubSettings,
      googleSettings: GoogleSettings,
      sys: ClassicActorSystemProvider): GrpcSubscriber =
    create(settings, googleSettings, sys.classicSystem)

  def create(settings: PubSubSettings, googleSettings: GoogleSettings, sys: ActorSystem): GrpcSubscriber =
    new GrpcSubscriber(settings, googleSettings, sys)

  /**
   * Creates a publisher with the new actors API.
   */
  def create(settings: PubSubSettings, sys: ClassicActorSystemProvider): GrpcSubscriber =
    create(settings, sys.classicSystem)

  def create(settings: PubSubSettings, sys: ActorSystem): GrpcSubscriber =
    new GrpcSubscriber(settings, GoogleSettings(sys), sys)

  /**
   * Creates a publisher with the new actors API.
   */
  def create(sys: ClassicActorSystemProvider): GrpcSubscriber = create(sys.classicSystem)

  def create(sys: ActorSystem): GrpcSubscriber =
    create(PubSubSettings(sys), sys)
}

/**
 * An extension that manages a single gRPC java subscriber client per actor system.
 */
final class GrpcSubscriberExt private (sys: ExtendedActorSystem) extends Extension {
  implicit val subscriber = GrpcSubscriber.create(sys)
}

object GrpcSubscriberExt extends ExtensionId[GrpcSubscriberExt] with ExtensionIdProvider {
  override def lookup = GrpcSubscriberExt
  override def createExtension(system: ExtendedActorSystem) = new GrpcSubscriberExt(system)

  /**
   * Java API
   *
   * Access to extension.
   */
  override def get(system: ActorSystem): GrpcSubscriberExt = super.get(system)

  /**
   * Java API
   *
   * Access to the extension from the new actors API.
   */
  override def get(system: ClassicActorSystemProvider): GrpcSubscriberExt = super.get(system.classicSystem)
}
