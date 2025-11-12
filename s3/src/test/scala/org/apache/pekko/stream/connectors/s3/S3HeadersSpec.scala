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

package org.apache.pekko.stream.connectors.s3

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.apache.pekko.stream.connectors.s3.impl.S3Request
import com.typesafe.config.ConfigFactory
import scala.reflect.ClassTag
import org.apache.pekko.stream.connectors.s3.impl.GetObject
import org.apache.pekko.stream.connectors.s3.impl.HeadObject
import org.apache.pekko.stream.connectors.s3.impl.PutObject
import org.apache.pekko.stream.connectors.s3.impl.InitiateMultipartUpload
import org.apache.pekko.stream.connectors.s3.impl.UploadPart
import org.apache.pekko.stream.connectors.s3.impl.CopyPart
import org.apache.pekko.stream.connectors.s3.impl.DeleteObject
import org.apache.pekko.stream.connectors.s3.impl.ListBucket
import org.apache.pekko.stream.connectors.s3.impl.MakeBucket
import org.apache.pekko.stream.connectors.s3.impl.DeleteBucket
import org.apache.pekko.stream.connectors.s3.impl.CheckBucket
import org.apache.pekko.stream.connectors.s3.impl.PutBucketVersioning
import org.apache.pekko.stream.connectors.s3.impl.GetBucketVersioning

class S3HeadersSpec extends AnyFlatSpecLike with Matchers {
  it should "filter headers based on what's allowed" in {
    val testOverrideConfig = ConfigFactory.parseString("""
      | pekko.connectors.s3.additional-allowed-headers {
      |    GetObject = [allowedExtra]
      |    HeadObject = [allowedExtra]
      |    PutObject = [allowedExtra]
      |    InitiateMultipartUpload = [allowedExtra]
      |    UploadPart = [allowedExtra]
      |    CopyPart = [allowedExtra]
      |    DeleteObject = [allowedExtra]
      |    ListBucket = [allowedExtra]
      |    MakeBucket = [allowedExtra]
      |    DeleteBucket = [allowedExtra]
      |    CheckBucket = [allowedExtra]
      |    PutBucketVersioning = [allowedExtra]
      |    GetBucketVersioning = [allowedExtra]
      | }
      |""".stripMargin)

    val defaultConfig = ConfigFactory.load()
    val finalConfig = testOverrideConfig.withFallback(defaultConfig)

    S3Request.allRequests.foreach {
      requestType =>
        val allowedHeaders = requestType.allowedHeaders.zipWithIndex.toMap.view.mapValues(_.toString()).toMap
        val extraHeaders = Map("allowedExtra" -> "allGood", "notAllowed" -> "shouldBeGone")
        val header = S3Headers().withCustomHeaders(allowedHeaders ++ extraHeaders)
        val s3Config = finalConfig.getConfig("pekko.connectors.s3")
        val headerFilter = header.headersFor(requestType)(S3Settings.apply(s3Config))
        val result = headerFilter.map(header => (header.name(), header.value()))
        result should contain allElementsOf (allowedHeaders.toSeq ++ Seq("allowedExtra" -> "allGood"))
    }

  }

  it should "be able to convert all headers toString and back correctly" in {
    val roundTrip = S3Request.allRequests
      .map(_.toString())
      .flatMap(S3Request.fromString(_))

    roundTrip should contain allElementsOf S3Request.allRequests

  }

  it should "contain all S3Request types" in {
    val actualSet = S3Request.allRequests

    actualSet should have size 13

    // Use exhaustive match to verify all are present
    def verifyAllPresent(req: S3Request): Boolean = req match {
      case e @ GetObject               => actualSet.contains(e)
      case e @ HeadObject              => actualSet.contains(e)
      case e @ PutObject               => actualSet.contains(e)
      case e @ InitiateMultipartUpload => actualSet.contains(e)
      case e @ UploadPart              => actualSet.contains(e)
      case e @ CopyPart                => actualSet.contains(e)
      case e @ DeleteObject            => actualSet.contains(e)
      case e @ ListBucket              => actualSet.contains(e)
      case e @ MakeBucket              => actualSet.contains(e)
      case e @ DeleteBucket            => actualSet.contains(e)
      case e @ CheckBucket             => actualSet.contains(e)
      case e @ PutBucketVersioning     => actualSet.contains(e)
      case e @ GetBucketVersioning     => actualSet.contains(e)
    }

    actualSet.foreach(req => verifyAllPresent(req) shouldBe true)
  
  }
}
