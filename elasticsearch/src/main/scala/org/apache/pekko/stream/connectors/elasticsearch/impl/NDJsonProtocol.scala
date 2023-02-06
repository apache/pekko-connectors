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

package org.apache.pekko.stream.connectors.elasticsearch.impl

import org.apache.pekko.http.scaladsl.model.{ ContentType, HttpCharsets, MediaType }

object NDJsonProtocol {

  val `application/x-ndjson`: MediaType.WithFixedCharset =
    MediaType.applicationWithFixedCharset("x-ndjson", HttpCharsets.`UTF-8`)
  val ndJsonContentType: ContentType.WithFixedCharset = ContentType.apply(`application/x-ndjson`)
}
