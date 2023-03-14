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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl

import org.apache.pekko.actor.{
  ClassicActorSystemProvider,
  ExtendedActorSystem,
  Extension,
  ExtensionId,
  ExtensionIdProvider
}
import org.apache.pekko.annotation.ApiMayChange
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryStorageSettings
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.impl.PekkoGrpcSettings
import com.google.cloud.bigquery.storage.v1.storage.BigQueryReadClient

/**
 * Holds the gRPC scala reader client instance.
 */
final class GrpcBigQueryStorageReader private (settings: BigQueryStorageSettings, sys: ClassicActorSystemProvider) {

  @ApiMayChange
  final val client = BigQueryReadClient(PekkoGrpcSettings.fromBigQuerySettings(settings)(sys))(sys)

  sys.classicSystem.registerOnTermination(client.close())
}

object GrpcBigQueryStorageReader {

  def apply(settings: BigQueryStorageSettings)(implicit sys: ClassicActorSystemProvider): GrpcBigQueryStorageReader =
    new GrpcBigQueryStorageReader(settings, sys)

  def apply()(implicit sys: ClassicActorSystemProvider): GrpcBigQueryStorageReader =
    apply(BigQueryStorageSettings(sys))
}

/**
 * An extension that manages a single gRPC scala reader client per actor system.
 */
final class GrpcBigQueryStorageReaderExt private (sys: ExtendedActorSystem) extends Extension {
  implicit val reader = GrpcBigQueryStorageReader()(sys)
}

object GrpcBigQueryStorageReaderExt extends ExtensionId[GrpcBigQueryStorageReaderExt] with ExtensionIdProvider {
  override def lookup = GrpcBigQueryStorageReaderExt
  override def createExtension(system: ExtendedActorSystem) = new GrpcBigQueryStorageReaderExt(system)

  /**
   * Access to extension.
   */
  def apply()(implicit system: ClassicActorSystemProvider): GrpcBigQueryStorageReaderExt = super.apply(system)

  /**
   * Java API
   *
   * Access to extension.
   */
  override def get(system: ClassicActorSystemProvider): GrpcBigQueryStorageReaderExt = super.get(system)
}
