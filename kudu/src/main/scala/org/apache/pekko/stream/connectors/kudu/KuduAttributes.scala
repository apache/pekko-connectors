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
import pekko.annotation.InternalApi
import pekko.stream.Attributes
import pekko.stream.Attributes.Attribute
import org.apache.kudu.client.KuduClient

/**
 * Pekko Stream attributes that are used when materializing Kudu stream blueprints.
 */
object KuduAttributes {

  /**
   * Kudu client to use for the stream
   */
  def client(client: KuduClient): Attributes = Attributes(new Client(client))

  final class Client @InternalApi private[KuduAttributes] (val client: KuduClient) extends Attribute
}
