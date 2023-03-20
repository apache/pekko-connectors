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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.e2e.javadsl

import org.apache.pekko
import pekko.stream.connectors.googlecloud.bigquery.e2e.scaladsl
import pekko.util.ccompat.JavaConverters._

abstract class EndToEndHelper extends scaladsl.EndToEndHelper {

  def getDatasetId = datasetId
  def getTableId = tableId
  def getRows = rows.asJava

}
