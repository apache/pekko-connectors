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

package org.apache.pekko.stream.connectors.elasticsearch

import org.scalatest.wordspec.AnyWordSpec

class ElasticsearchParamsSpec extends AnyWordSpec {
  "elasticsearchParams" should {
    "not allow setting a null indexName for API version V5" in {
      assertThrows[IllegalArgumentException] {
        ElasticsearchParams.V5(null, "_doc")
      }
    }

    "not allow setting a null typeName for API version V5" in {
      assertThrows[IllegalArgumentException] {
        ElasticsearchParams.V5("index", null)
      }
    }

    "not allow setting a null indexName for API version V7" in {
      assertThrows[IllegalArgumentException] {
        ElasticsearchParams.V7(null)
      }
    }
  }
}
