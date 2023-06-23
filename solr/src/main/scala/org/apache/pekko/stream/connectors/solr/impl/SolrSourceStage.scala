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

package org.apache.pekko.stream.connectors.solr.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.stage.{ GraphStage, GraphStageLogic, OutHandler }
import pekko.stream.{ Attributes, Outlet, SourceShape }
import org.apache.solr.client.solrj.io.Tuple
import org.apache.solr.client.solrj.io.stream.TupleStream

import scala.util.control.NonFatal

/**
 * Internal API
 */
@InternalApi
private[solr] final class SolrSourceStage(tupleStream: TupleStream) extends GraphStage[SourceShape[Tuple]] {
  val out: Outlet[Tuple] = Outlet("SolrSource.out")
  override val shape: SourceShape[Tuple] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler {

      setHandler(out, this)

      override def preStart(): Unit =
        try {
          tupleStream.open()
        } catch {
          case NonFatal(exc) =>
            failStage(exc)
        }

      override def postStop(): Unit =
        try {
          tupleStream.close()
        } catch {
          case NonFatal(exc) =>
            failStage(exc)
        }

      override def onPull(): Unit = fetchFromSolr()

      private def fetchFromSolr(): Unit = {
        val tuple = tupleStream.read()
        if (tuple.EOF) {
          completeStage()
        } else if (tuple.EXCEPTION) {
          failStage(new IllegalStateException(tuple.getException))
        } else {
          emit(out, tuple)
        }
      }

    }
}
