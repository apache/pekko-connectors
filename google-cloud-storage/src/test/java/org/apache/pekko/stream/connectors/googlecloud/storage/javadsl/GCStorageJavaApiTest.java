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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.Done;
import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.stream.connectors.googlecloud.storage.Bucket;
import org.apache.pekko.stream.connectors.googlecloud.storage.CustomerEncryption;
import org.apache.pekko.stream.connectors.googlecloud.storage.FailedUpload;
import org.apache.pekko.stream.connectors.googlecloud.storage.ObjectAccessControls;
import org.apache.pekko.stream.connectors.googlecloud.storage.Owner;
import org.apache.pekko.stream.connectors.googlecloud.storage.ProjectTeam;
import org.apache.pekko.stream.connectors.googlecloud.storage.StorageObject;
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

  @Test
  public void storageObjectCanBeCreatedFromJavaTypes() {
    // pretyped with the concrete collection types to check that the factory accepts subtypes
    Optional<HashMap<String, String>> metadata = Optional.of(new HashMap<>());
    metadata.get().put("key", "value");
    ObjectAccessControls control =
        ObjectAccessControls.create(
            "storage#objectAccessControl",
            "id",
            "selfLink",
            "bucket",
            "object",
            "1",
            "entity",
            "OWNER",
            "email",
            "entityId",
            "domain",
            ProjectTeam.create("1", "team"),
            "etag");
    Optional<ArrayList<ObjectAccessControls>> acl = Optional.of(new ArrayList<>());
    acl.get().add(control);
    OffsetDateTime now = OffsetDateTime.now();

    StorageObject storageObject =
        StorageObject.createFromJava(
            "storage#object",
            "id",
            "name",
            "bucket",
            1L,
            ContentTypes.APPLICATION_OCTET_STREAM,
            10L,
            "etag",
            "md5Hash",
            "crc32c",
            "mediaLink",
            "selfLink",
            now,
            now,
            Optional.empty(),
            "STANDARD",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            1L,
            Optional.of(Boolean.TRUE),
            Optional.of(Boolean.FALSE),
            Optional.empty(),
            now,
            Optional.empty(),
            Optional.empty(),
            metadata,
            Optional.of(3),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            acl);

    assertEquals(Optional.of(Boolean.TRUE), storageObject.getTemporaryHold());
    assertEquals(Optional.of(Boolean.FALSE), storageObject.getEventBasedHold());
    assertEquals(
        Optional.of(Collections.singletonMap("key", "value")), storageObject.getMetadata());
    assertEquals(Optional.of(3), storageObject.getComponentCount());
    assertEquals(Optional.of(Collections.singletonList(control)), storageObject.getAcl());
  }

  /** Compile-only check: bucket operations don't require attributes. */
  @SuppressWarnings("unused")
  private static void bucketOperationsCompile(ClassicActorSystemProvider system) {
    CompletionStage<Optional<Bucket>> get = GCStorage.getBucket("bucket", system);
    CompletionStage<Bucket> create = GCStorage.createBucket("bucket", "location", system);
    CompletionStage<Done> delete = GCStorage.deleteBucket("bucket", system);
  }
}
