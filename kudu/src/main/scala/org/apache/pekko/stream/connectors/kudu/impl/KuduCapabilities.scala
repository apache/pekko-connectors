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

package org.apache.pekko.stream.connectors.kudu.impl

import org.apache.pekko.stream.stage.StageLogging
import org.apache.kudu.client._
import org.apache.kudu.Schema
import org.apache.kudu.client.KuduTable

/**
 * INTERNAL API
 */
private trait KuduCapabilities {
  this: StageLogging =>

  protected def getOrCreateTable(kuduClient: KuduClient,
      tableName: String,
      schema: Schema,
      createTableOptions: CreateTableOptions): KuduTable =
    if (kuduClient.tableExists(tableName))
      kuduClient.openTable(tableName)
    else {
      kuduClient.createTable(tableName, schema, createTableOptions)
      log.info("Table {} created with columns: {}.", tableName, schema)
      kuduClient.openTable(tableName)
    }

}
