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

package org.apache.pekko.stream.connectors.googlecloud.storage.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.Done;
import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.stream.connectors.googlecloud.storage.Bucket;
import org.apache.pekko.stream.connectors.googlecloud.storage.CustomerEncryption;
import org.apache.pekko.stream.connectors.googlecloud.storage.FailedUpload;
import org.apache.pekko.stream.connectors.googlecloud.storage.Owner;
import org.apache.pekko.stream.connectors.googlecloud.storage.ProjectTeam;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the Google Cloud Storage model without Google Cloud Storage. */
public class GCStorageJavaApiTest {

  @Test
  public void modelContentsCanBeRead() {
    Owner owner = Owner.create("entity", Optional.of("id"));
    assertEquals("entity", owner.getEntity());
    assertEquals(Optional.of("id"), owner.getEntityId());

    ProjectTeam team = ProjectTeam.create("1", "team");
    assertEquals("1", team.getProjectNumber());
    assertEquals("team", team.getTeam());

    CustomerEncryption encryption = CustomerEncryption.create("AES256", "sha");
    assertEquals("AES256", encryption.getEncryptionAlgorithm());
    assertEquals("sha", encryption.getKeySha256());
  }

  @Test
  public void failedUploadAcceptsThrowableSubtypes() {
    RuntimeException reason = new RuntimeException("boom");
    java.util.List<RuntimeException> reasons = java.util.Collections.singletonList(reason);
    assertEquals(reason, FailedUpload.create(reasons).getReasons().get(0));
  }

  /** Compile-only check: bucket operations don't require attributes. */
  @SuppressWarnings("unused")
  private static void bucketOperationsCompile(ClassicActorSystemProvider system) {
    CompletionStage<Optional<Bucket>> get = GCStorage.getBucket("bucket", system);
    CompletionStage<Bucket> create = GCStorage.createBucket("bucket", "location", system);
    CompletionStage<Done> delete = GCStorage.deleteBucket("bucket", system);
  }
}
