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
import pekko.NotUsed
import pekko.stream.connectors.solr.impl.SolrFlowStage
import pekko.stream.connectors.solr.{ SolrUpdateSettings, WriteMessage, WriteResult }
import pekko.stream.scaladsl.Flow
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.common.SolrInputDocument

/**
 * Scala API
 */
object SolrFlow {

  /**
   * Write `SolrInputDocument`s to Solr in a flow emitting `WriteResult`s containing the status.
   */
  def documents(
      collection: String,
      settings: SolrUpdateSettings)(
      implicit client: SolrClient): Flow[Seq[WriteMessage[SolrInputDocument, NotUsed]],
    Seq[
      WriteResult[SolrInputDocument, NotUsed]], NotUsed] =
    Flow
      .fromGraph(
        new SolrFlowStage[SolrInputDocument, NotUsed](
          collection,
          client,
          settings,
          identity))

  /**
   * Write Java bean stream elements to Solr in a flow emitting `WriteResult`s containing the status.
   * The stream element classes must be annotated for use with [[org.apache.solr.client.solrj.beans.DocumentObjectBinder]] for conversion.
   */
  def beans[T](
      collection: String,
      settings: SolrUpdateSettings)(
      implicit client: SolrClient)
      : Flow[Seq[WriteMessage[T, NotUsed]], Seq[WriteResult[T, NotUsed]], NotUsed] =
    Flow
      .fromGraph(
        new SolrFlowStage[T, NotUsed](
          collection,
          client,
          settings,
          new DefaultSolrObjectBinder(client)))

  /**
   * Write stream elements to Solr in a flow emitting `WriteResult`s containing the status.
   *
   * @param binder a conversion function to create `SolrInputDocument`s of the stream elements
   */
  def typeds[T](
      collection: String,
      settings: SolrUpdateSettings,
      binder: T => SolrInputDocument)(
      implicit client: SolrClient)
      : Flow[Seq[WriteMessage[T, NotUsed]], Seq[WriteResult[T, NotUsed]], NotUsed] =
    Flow
      .fromGraph(
        new SolrFlowStage[T, NotUsed](
          collection,
          client,
          settings,
          binder))

  /**
   * Write `SolrInputDocument`s to Solr in a flow emitting `WriteResult`s containing the status.
   *
   * @tparam PT pass-through type
   */
  def documentsWithPassThrough[PT](
      collection: String,
      settings: SolrUpdateSettings)(
      implicit client: SolrClient): Flow[Seq[WriteMessage[SolrInputDocument, PT]],
    Seq[WriteResult[SolrInputDocument, PT]], NotUsed] =
    Flow
      .fromGraph(
        new SolrFlowStage[SolrInputDocument, PT](
          collection,
          client,
          settings,
          identity))

  /**
   * Write Java bean stream elements to Solr in a flow emitting `WriteResult`s containing the status.
   * The stream element classes must be annotated for use with [[org.apache.solr.client.solrj.beans.DocumentObjectBinder]] for conversion.
   *
   * @tparam PT pass-through type
   */
  def beansWithPassThrough[T, PT](
      collection: String,
      settings: SolrUpdateSettings)(implicit client: SolrClient)
      : Flow[Seq[WriteMessage[T, PT]], Seq[WriteResult[T, PT]], NotUsed] =
    Flow
      .fromGraph(
        new SolrFlowStage[T, PT](
          collection,
          client,
          settings,
          new DefaultSolrObjectBinder(client)))

  /**
   * Write stream elements to Solr in a flow emitting `WriteResult`s containing the status.
   *
   * @param binder a conversion function to create `SolrInputDocument`s of the stream elements
   * @tparam PT pass-through type
   */
  def typedsWithPassThrough[T, PT](
      collection: String,
      settings: SolrUpdateSettings,
      binder: T => SolrInputDocument)(implicit client: SolrClient)
      : Flow[Seq[WriteMessage[T, PT]], Seq[WriteResult[T, PT]], NotUsed] =
    Flow
      .fromGraph(
        new SolrFlowStage[T, PT](
          collection,
          client,
          settings,
          binder))

  private class DefaultSolrObjectBinder(solrClient: SolrClient) extends (Any => SolrInputDocument) {
    override def apply(v1: Any): SolrInputDocument =
      solrClient.getBinder.toSolrInputDocument(v1)
  }

}
