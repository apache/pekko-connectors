/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.stream.connectors.s3

import markatta.futiles.Retry
import org.apache.commons.lang3.StringUtils
import org.apache.pekko
import org.apache.pekko.stream.connectors.s3.scaladsl.S3
import org.apache.pekko.stream.scaladsl.Sink
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
