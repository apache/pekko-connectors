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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.connectors.google.GoogleSettings
import org.apache.pekko.stream.connectors.googlecloud.storage.scaladsl.GCStorage
import org.apache.pekko.stream.connectors.googlecloud.storage.{ FakeGCSServerTest, GCSSettings, GCStorageSettings }

class FakeGCStorageStreamIntegrationSpec extends GCStorageStreamIntegrationSpec with FakeGCSServerTest {
  override def settings: GoogleSettings = googleSettings
  override def gcsSettings: GCSSettings = gCSSettings

  override implicit def system: ActorSystem = actorSystem

  override def bucket = "connectors"
  override def rewriteBucket = "pekko-connectors-rewrite"
  override def projectId = settings.projectId

}
