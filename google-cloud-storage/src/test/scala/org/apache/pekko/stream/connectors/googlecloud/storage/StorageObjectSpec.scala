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

import java.time.OffsetDateTime
import java.util.Optional

import scala.annotation.nowarn

import org.apache.pekko
import pekko.http.javadsl.model.ContentTypes
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StorageObjectSpec extends AnyFlatSpec with Matchers {

  private val now = OffsetDateTime.now()

  private val control = ObjectAccessControls(
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
    ProjectTeam("1", "team"),
    "etag")

  "The deprecated StorageObject.create" should "still take Scala types for its optional values" in {
    // callers compiled against earlier versions pass Scala values through the erased Optional parameters
    @nowarn("cat=deprecation")
    val storageObject = StorageObject.create(
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
      Optional.empty[OffsetDateTime](),
      "STANDARD",
      Optional.empty[String](),
      Optional.empty[String](),
      Optional.empty[String](),
      1L,
      Optional.of(true),
      Optional.of(false),
      Optional.empty[OffsetDateTime](),
      now,
      Optional.empty[String](),
      Optional.empty[OffsetDateTime](),
      Optional.of(Map("key" -> "value")),
      Optional.of(3),
      Optional.empty[String](),
      Optional.empty[CustomerEncryption](),
      Optional.empty[Owner](),
      Optional.of(List(control)))

    storageObject.temporaryHold shouldBe Some(true)
    storageObject.eventBasedHold shouldBe Some(false)
    storageObject.metadata shouldBe Some(Map("key" -> "value"))
    storageObject.componentCount shouldBe Some(3)
    storageObject.acl shouldBe Some(List(control))
  }
}
