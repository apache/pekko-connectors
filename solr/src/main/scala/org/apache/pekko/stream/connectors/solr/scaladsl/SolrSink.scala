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

package org.apache.pekko.stream.connectors.solr.scaladsl

import org.apache.pekko
import pekko.stream.connectors.solr.{ SolrUpdateSettings, WriteMessage }
import pekko.stream.scaladsl.{ Keep, Sink }
import pekko.{ Done, NotUsed }
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.common.SolrInputDocument

import scala.collection.immutable
import scala.concurrent.Future

/**
 * Scala API
 */
object SolrSink {

  /**
   * Write `SolrInputDocument`s to Solr.
   */
  def documents[T](collection: String, settings: SolrUpdateSettings)(
      implicit client: SolrClient): Sink[immutable.Seq[WriteMessage[SolrInputDocument, NotUsed]], Future[Done]] =
    SolrFlow
      .documents(collection, settings)
      .toMat(Sink.ignore)(Keep.right)

  /**
   * Write Java bean stream elements to Solr.
   * The stream element classes must be annotated for use with [[org.apache.solr.client.solrj.beans.DocumentObjectBinder]] for conversion.
   */
  def beans[T](collection: String, settings: SolrUpdateSettings)(
      implicit client: SolrClient): Sink[immutable.Seq[WriteMessage[T, NotUsed]], Future[Done]] =
    SolrFlow
      .beans[T](collection, settings)
      .toMat(Sink.ignore)(Keep.right)

  /**
   * Write stream elements to Solr.
   *
   * @param binder a conversion function to create `SolrInputDocument`s of the stream elements
   */
  def typeds[T](
      collection: String,
      settings: SolrUpdateSettings,
      binder: T => SolrInputDocument)(
      implicit client: SolrClient): Sink[immutable.Seq[WriteMessage[T, NotUsed]], Future[Done]] =
    SolrFlow
      .typeds[T](collection, settings, binder)
      .toMat(Sink.ignore)(Keep.right)

}
