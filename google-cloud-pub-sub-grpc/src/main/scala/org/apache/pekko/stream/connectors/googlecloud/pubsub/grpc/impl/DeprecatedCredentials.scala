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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.impl

import org.apache.pekko.annotation.InternalApi
import io.grpc.{ CallCredentials, Metadata }

import java.util.concurrent.Executor

/**
 * Marker class indicating that credentials should be resolved via GoogleSettings.
 */
@InternalApi
private[grpc] final case class DeprecatedCredentials(underlying: CallCredentials) extends CallCredentials {

  override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo,
      appExecutor: Executor,
      applier: CallCredentials.MetadataApplier): Unit =
    underlying.applyRequestMetadata(requestInfo, appExecutor, applier)

  override def thisUsesUnstableApi(): Unit = underlying.thisUsesUnstableApi()
}

@InternalApi
private[grpc] object DeprecatedCredentials {

  /** A no-op marker instance used to signal "resolve via GoogleSettings". */
  val marker: CallCredentials = DeprecatedCredentials(new CallCredentials {
    override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo,
        appExecutor: Executor,
        applier: CallCredentials.MetadataApplier): Unit =
      applier.apply(new Metadata())

    override def thisUsesUnstableApi(): Unit = ()
  })
}
