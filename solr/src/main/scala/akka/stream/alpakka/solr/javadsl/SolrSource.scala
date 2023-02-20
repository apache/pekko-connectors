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

package akka.stream.alpakka.solr.javadsl

import akka.NotUsed
import akka.stream.alpakka.solr.impl.SolrSourceStage
import akka.stream.javadsl.Source
import org.apache.solr.client.solrj.io.Tuple
import org.apache.solr.client.solrj.io.stream.TupleStream

/**
 * Java API
 */
object SolrSource {

  /**
   * Use a Solr [[org.apache.solr.client.solrj.io.stream.TupleStream]] as source.
   */
  def fromTupleStream(ts: TupleStream): Source[Tuple, NotUsed] =
    Source.fromGraph(new SolrSourceStage(ts))
}
