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

package org.apache.pekko.stream.connectors.ftp.impl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CommonFtpOperationsSpec extends AnyFlatSpec with Matchers {

  "CommonFtpOperations.validatePath" should "accept normal relative path" in {
    CommonFtpOperations.validatePath("dir/file.txt", "path") // no exception
  }

  it should "accept simple filename" in {
    CommonFtpOperations.validatePath("file.txt", "path") // no exception
  }

  it should "reject null" in {
    an[IllegalArgumentException] should be thrownBy {
      CommonFtpOperations.validatePath(null, "path")
    }
  }

  it should "reject dot-dot at start" in {
    an[IllegalArgumentException] should be thrownBy {
      CommonFtpOperations.validatePath("../etc/passwd", "path")
    }
  }

  it should "reject dot-dot in middle" in {
    an[IllegalArgumentException] should be thrownBy {
      CommonFtpOperations.validatePath("dir/../../etc/passwd", "path")
    }
  }

  it should "reject dot-dot at end" in {
    an[IllegalArgumentException] should be thrownBy {
      CommonFtpOperations.validatePath("dir/..", "path")
    }
  }

  "CommonFtpOperations.concatPath" should "concatenate simple paths" in {
    CommonFtpOperations.concatPath("/base", "file.txt") shouldBe "/base/file.txt"
  }

  it should "handle trailing slash on base path" in {
    CommonFtpOperations.concatPath("/base/", "file.txt") shouldBe "/base/file.txt"
  }

  it should "handle nested names" in {
    CommonFtpOperations.concatPath("/base", "subdir/file.txt") shouldBe "/base/subdir/file.txt"
  }

  it should "reject dot-dot in name" in {
    an[IllegalArgumentException] should be thrownBy {
      CommonFtpOperations.concatPath("/base", "../etc/passwd")
    }
  }

  it should "reject dot-dot in nested name" in {
    an[IllegalArgumentException] should be thrownBy {
      CommonFtpOperations.concatPath("/base", "subdir/../../etc/passwd")
    }
  }

  it should "reject absolute name" in {
    an[IllegalArgumentException] should be thrownBy {
      CommonFtpOperations.concatPath("/base", "/etc/passwd")
    }
  }

  it should "reject null name" in {
    an[IllegalArgumentException] should be thrownBy {
      CommonFtpOperations.concatPath("/base", null)
    }
  }

  it should "handle dot segments without platform normalization" in {
    CommonFtpOperations.concatPath("/base", "./file.txt") shouldBe "/base/./file.txt"
  }

  it should "allow names under root path" in {
    CommonFtpOperations.concatPath("/", "sample_dir") shouldBe "/sample_dir"
  }
}
