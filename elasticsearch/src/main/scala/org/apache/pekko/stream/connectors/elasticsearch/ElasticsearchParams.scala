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

final class ElasticsearchParams private (val indexName: String, val typeName: Option[String]) {
  override def toString =
    s"""ElasticsearchParams(indexName=$indexName,typeName=$typeName)"""
}

object ElasticsearchParams {
  def V7(indexName: String): ElasticsearchParams = {
    require(indexName != null, "You must define an index name")

    new ElasticsearchParams(indexName, None)
  }

  /**
   * Elasticsearch 8 dropped mapping types, so the params are shaped like the V7 ones.
   */
  def V8(indexName: String): ElasticsearchParams = V7(indexName)

  /**
   * Elasticsearch 9 dropped mapping types, so the params are shaped like the V7 ones.
   */
  def V9(indexName: String): ElasticsearchParams = V7(indexName)

  def V5(indexName: String, typeName: String): ElasticsearchParams = {
    require(indexName != null, "You must define an index name")
    require(typeName != null && typeName.trim.nonEmpty, "You must define a type name for ElasticSearch API version V5")

    new ElasticsearchParams(indexName, Some(typeName))
  }
}
