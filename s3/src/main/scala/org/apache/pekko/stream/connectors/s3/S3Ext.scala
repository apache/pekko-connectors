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

package org.apache.pekko.stream.connectors.s3

import org.apache.pekko
import pekko.actor.{ ClassicActorSystemProvider, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }

/**
 * Manages one [[S3Settings]] per `ActorSystem`.
 */
final class S3Ext private (sys: ExtendedActorSystem) extends Extension {
  val settings: S3Settings = settings(S3Settings.ConfigPath)

  def settings(prefix: String): S3Settings = S3Settings(sys.settings.config.getConfig(prefix))
}

object S3Ext extends ExtensionId[S3Ext] with ExtensionIdProvider {
  override def lookup: S3Ext.type = S3Ext
  override def createExtension(system: ExtendedActorSystem) = new S3Ext(system)

  /**
   * Java API.
   * Get the S3 extension with the classic actors API.
   */
  override def get(system: pekko.actor.ActorSystem): S3Ext = super.apply(system)

  /**
   * Java API.
   * Get the S3 extension with the new actors API.
   */
  override def get(system: ClassicActorSystemProvider): S3Ext = super.apply(system)
}
