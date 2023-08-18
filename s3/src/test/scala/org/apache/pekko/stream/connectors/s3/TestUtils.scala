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

import org.apache.commons.lang3.StringUtils
import org.apache.pekko
import pekko.stream.connectors.s3.impl.retry.Retry
import pekko.stream.connectors.s3.scaladsl.S3
import pekko.stream.scaladsl.Sink
import org.scalacheck.Gen
import pekko.actor.ActorSystem
import pekko.stream.Attributes

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

object TestUtils {
  def checkEnvVarAvailable(env: String): Boolean =
    try
      sys.env.get(env) match {
        case Some(value) if StringUtils.isBlank(value) => false
        case Some(_)                                   => true
        case None                                      => false
      }
    catch {
      case _: NoSuchElementException => false
    }

  def cleanAndDeleteBucket(bucket: String, s3Attrs: Attributes)(implicit system: ActorSystem): Future[Unit] = {
    implicit val ec: ExecutionContext = system.dispatcher
    for {
      bucketVersioningResult <- S3.getBucketVersioning(bucket)(implicitly, s3Attrs)
      deleteAllVersions = bucketVersioningResult.status.contains(BucketVersioningStatus.Enabled)
      _ <- S3.deleteBucketContents(bucket, deleteAllVersions = deleteAllVersions).withAttributes(s3Attrs).runWith(
        Sink.ignore)
      multiParts <-
        S3.listMultipartUpload(bucket, None).withAttributes(s3Attrs).runWith(Sink.seq)
      _ <- Future.sequence(multiParts.map { part =>
        S3.deleteUpload(bucket, part.key, part.uploadId)(implicitly, s3Attrs)
      })
      _ <- Retry.retryWithBackOff(
        5,
        100.millis,
        throwable => throwable.getMessage.contains("The bucket you tried to delete is not empty"))(
        S3.deleteBucket(bucket)(implicitly, s3Attrs))
      _ = system.log.info(s"Completed deleting bucket $bucket")
    } yield ()
  }

  /**
   * Will return a single value from a given generator. WARNING this will block the thread
   * until a value is retrieved so only use this for generators that have a high chance of
   * returning a value. If a [[Gen.filter]] ends up filtering out too many values this can
   * cause the thread to be stuck for a long time
   */
  @tailrec
  def loopUntilGenRetrievesValue[T](gen: Gen[T]): T =
    gen.sample match {
      case Some(value) => value
      case None        => loopUntilGenRetrievesValue(gen)
    }

}
