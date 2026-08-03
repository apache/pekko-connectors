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

package org.apache.pekko.stream.connectors.file.impl.archive

import org.apache.pekko.stream.connectors.file.ZipArchiveMetadata
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ZipArchiveMetadataSpec extends AnyFlatSpec with Matchers {

  "ZipArchiveMetadata" should "reject dot-dot in name" in {
    an[IllegalArgumentException] should be thrownBy {
      ZipArchiveMetadata("../../etc/passwd")
    }
  }

  it should "reject absolute path" in {
    an[IllegalArgumentException] should be thrownBy {
      ZipArchiveMetadata("/etc/passwd")
    }
  }

  it should "reject dot-dot in middle of path" in {
    an[IllegalArgumentException] should be thrownBy {
      ZipArchiveMetadata("dir/../../../etc/crontab")
    }
  }

  it should "accept normal relative paths" in {
    val meta = ZipArchiveMetadata("dir/subdir/file.txt")
    meta.name shouldBe "dir/subdir/file.txt"
  }

  it should "accept simple filename" in {
    val meta = ZipArchiveMetadata("file.txt")
    meta.name shouldBe "file.txt"
  }
}
