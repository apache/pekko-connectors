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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray

import org.apache.pekko.util.ByteString
import spray.json.JsString
import java.nio.charset.StandardCharsets.US_ASCII

import scala.util.Try

private[spray] object BigQueryBytes {

  def unapply(bytes: JsString): Option[ByteString] = Try(ByteString(bytes.value, US_ASCII).decodeBase64).toOption

}
