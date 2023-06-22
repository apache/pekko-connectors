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

package org.apache.pekko.stream.connectors.couchbase

import org.apache.pekko.annotation.InternalApi
import com.couchbase.client.java.document.json.JsonObject

/**
 * Describes a Couchbase related failure with an error code.
 */
final class CouchbaseResponseException(msg: String, val code: Option[Int]) extends RuntimeException(msg) {

  override def toString = s"CouchbaseResponseException($msg, $code)"
}

/** INTERNAL API */
@InternalApi
private[pekko] object CouchbaseResponseException {
  def apply(json: JsonObject): CouchbaseResponseException =
    new CouchbaseResponseException(
      msg = if (json.containsKey("msg")) json.getString("msg") else "",
      code = if (json.containsKey("code")) Some(json.getInt("code")) else None)
}
