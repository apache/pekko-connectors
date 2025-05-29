/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.googlecloud.storage

import com.dimafeng.testcontainers.ForAllTestContainer
import org.apache.pekko
import org.apache.pekko.stream.connectors.google.auth.NoCredentials
import org.apache.pekko.stream.connectors.google.{ GoogleSettings, RequestSettings, RetrySettings }
import org.apache.pekko.testkit.TestKitBase
import org.scalatest.Suite

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Optional

trait FakeGCSServerTest extends ForAllTestContainer with TestKitBase { self: Suite =>
  override lazy val container: FakeGCSContainer =
    new FakeGCSContainer(List("connectors", "pekko-connectors-rewrite"), Some("europe-west1"))

  lazy val googleSettings =
    GoogleSettings.create(
      "test-project",
      NoCredentials("", ""),
      RequestSettings.create(
        Optional.empty(),
        Optional.empty(),
        prettyPrint = false,
        15728640, // 15 MiB
        RetrySettings.create(
          6,
          Duration.of(1, ChronoUnit.SECONDS),
          Duration.of(1, ChronoUnit.MINUTES),
          0.2d
        ),
        Optional.empty()
      )
    )

  lazy val gCSSettings =
    GCSSettings(container.getHostAddress, GCSSettings().basePath)
}
