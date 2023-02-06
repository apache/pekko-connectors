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

package org.apache.pekko.stream.connectors.elasticsearch.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko.stream.connectors.elasticsearch._
import org.apache.pekko.stream.javadsl._
import org.apache.pekko.{ Done, NotUsed }
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Java API to create Elasticsearch sinks.
 */
object ElasticsearchSink {

  /**
   * Create a sink to update Elasticsearch with [[org.apache.pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`.
   */
  def create[T](
      elasticsearchParams: ElasticsearchParams,
      settings: WriteSettingsBase[_, _],
      objectMapper: ObjectMapper)
      : org.apache.pekko.stream.javadsl.Sink[WriteMessage[T, NotUsed], CompletionStage[Done]] =
    ElasticsearchFlow
      .create(elasticsearchParams, settings, objectMapper)
      .toMat(Sink.ignore[WriteResult[T, NotUsed]](), Keep.right[NotUsed, CompletionStage[Done]])

}
