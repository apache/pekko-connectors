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

package org.apache.pekko.stream.connectors.elasticsearch

/**
 * Opensearch 1.x is fully compatible with Elasticsearch 7.x release line, so we could
 * reuse the Elasticsearch V7 compatibile implementation.
 */
object OpensearchParams {
  def V1(indexName: String): ElasticsearchParams = ElasticsearchParams.V7(indexName)
}
