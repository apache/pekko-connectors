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

package org.apache.pekko.stream.connectors.elasticsearch.scaladsl

import org.apache.pekko.stream.connectors.elasticsearch._
import org.apache.pekko.stream.scaladsl.{ Keep, Sink }
import org.apache.pekko.{ Done, NotUsed }
import spray.json.JsonWriter

import scala.concurrent.Future

/**
 * Scala API to create Elasticsearch sinks.
 */
object ElasticsearchSink {

  /**
   * Create a sink to update Elasticsearch with [[org.apache.pekko.stream.connectors.elasticsearch.WriteMessage WriteMessage]]s containing type `T`.
   */
  def create[T](elasticsearchParams: ElasticsearchParams, settings: WriteSettingsBase[_, _])(
      implicit sprayJsonWriter: JsonWriter[T]): Sink[WriteMessage[T, NotUsed], Future[Done]] =
    ElasticsearchFlow.create[T](elasticsearchParams, settings).toMat(Sink.ignore)(Keep.right)

}
