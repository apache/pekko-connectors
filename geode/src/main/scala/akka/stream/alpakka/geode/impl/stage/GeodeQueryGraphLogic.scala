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

package akka.stream.alpakka.geode.impl.stage

import akka.annotation.InternalApi
import akka.stream.SourceShape
import akka.stream.stage.StageLogging
import org.apache.geode.cache.client.ClientCache
import org.apache.geode.cache.query.SelectResults

import scala.util.Try

@InternalApi
private[geode] abstract class GeodeQueryGraphLogic[V](val shape: SourceShape[V],
    val clientCache: ClientCache,
    val query: String)
    extends GeodeSourceStageLogic[V](shape, clientCache)
    with StageLogging {

  override def executeQuery() = Try {
    qs.newQuery(query)
      .execute()
      .asInstanceOf[SelectResults[V]]
      .iterator()
  }

}
