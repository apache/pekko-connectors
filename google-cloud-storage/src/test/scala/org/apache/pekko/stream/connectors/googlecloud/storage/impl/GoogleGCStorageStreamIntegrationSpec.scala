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

package org.apache.pekko.stream.connectors.googlecloud.storage.impl

import org.apache.pekko.stream.connectors.google.GoogleSettings
import org.apache.pekko.stream.connectors.googlecloud.storage.GCSSettings
import org.scalatest.DoNotDiscover

import scala.annotation.nowarn

/**
 * USAGE
 * - Create a google cloud service account
 * - Make sure it has these roles:
 *    storage object creator
 *    storage object viewer
 *    storage object admin
 *    storage admin (to run the create/delete bucket test)
 * - modify test/resources/application.conf
 * - create a `pekko-connectors` bucket for testing
 * - create a rewrite `pekko-connectors-rewrite` bucket for testing
 *
 * Since this a test that is marked with `@DoNotDiscover` it will not run as part of the test suite unless its
 * explicitly executed with
 * `sbt "google-cloud-storage/Test/runMain org.scalatest.tools.Runner -o -s org.apache.pekko.stream.connectors.googlecloud.storage.impl.GoogleGCStorageStreamIntegrationSpec"`
 *
 * See https://www.scalatest.org/user_guide/using_the_runner for more details about the runner.
 *
 * Running tests directly via IDE's such as Intellij is also supported
 */
@DoNotDiscover
class GoogleGCStorageStreamIntegrationSpec extends GCStorageStreamIntegrationSpec {
  override def settings: GoogleSettings = GoogleSettings()
  override def gcsSettings: GCSSettings = GCSSettings()

  override def bucket = "connectors"
  override def rewriteBucket = "pekko-connectors-rewrite"
  override def projectId = settings.projectId
}
