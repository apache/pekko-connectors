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

package org.apache.pekko.stream.connectors.kudu

import org.apache.pekko
import pekko.actor.{ ClassicActorSystemProvider, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import org.apache.kudu.client.KuduClient
import org.apache.kudu.client.AsyncKuduClient.EncryptionPolicy

/**
 * Manages one [[org.apache.kudu.client.KuduClient]] per `ActorSystem`.
 */
final class KuduClientExt private (sys: ExtendedActorSystem) extends Extension {
  val client = {
    val masterAddress = sys.settings.config.getString("pekko.connectors.kudu.master-address")
    new KuduClient.KuduClientBuilder(masterAddress)
      .encryptionPolicy(EncryptionPolicy.valueOf(
        sys.settings.config.getString("pekko.connectors.kudu.encryptionPolicy"))).build()
  }
  sys.registerOnTermination(client.shutdown())
}

object KuduClientExt extends ExtensionId[KuduClientExt] with ExtensionIdProvider {
  override def lookup = KuduClientExt
  override def createExtension(system: ExtendedActorSystem) = new KuduClientExt(system)

  /**
   * Get the Kudu Client extension with the classic actors API.
   * Java API.
   */
  override def get(system: pekko.actor.ActorSystem): KuduClientExt = super.get(system)

  /**
   * Get the Kudu Client extension with the new actors API.
   * Java API.
   */
  override def get(system: ClassicActorSystemProvider): KuduClientExt = super.get(system)
}
