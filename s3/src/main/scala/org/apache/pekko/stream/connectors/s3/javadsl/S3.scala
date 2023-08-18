/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.s3.javadsl

import java.util.Optional
import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.actor.ClassicActorSystemProvider
import pekko.japi.{ Pair => JPair }
import pekko.{ Done, NotUsed }
import pekko.http.javadsl.model._
import pekko.http.javadsl.model.headers.ByteRange
import pekko.http.scaladsl.model.headers.{ ByteRange => ScalaByteRange }
import pekko.http.scaladsl.model.{ ContentType => ScalaContentType, HttpMethod => ScalaHttpMethod }
import pekko.stream.{ Attributes, SystemMaterializer }
import pekko.stream.connectors.s3.headers.{ CannedAcl, ServerSideEncryption }
import pekko.stream.connectors.s3._
import pekko.stream.connectors.s3.impl._
import pekko.stream.javadsl.{ RunnableGraph, Sink, Source }
import pekko.stream.scaladsl.SourceToCompletionStage
import pekko.util.ccompat.JavaConverters._
import pekko.util.ByteString
import pekko.util.OptionConverters._
import pekko.util.FutureConverters._

/**
 * Java API
 *
 * Factory of S3 operations.
 */
object S3 {

  /**
   * Use this for a low level access to S3.
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param method the [[pekko.http.javadsl.model.HttpMethod HttpMethod]] to use when making the request
   * @param s3Headers any headers you want to add
   * @return a raw HTTP response from S3
   */
  def request(bucket: String, key: String, method: HttpMethod, s3Headers: S3Headers): Source[HttpResponse, NotUsed] =
    request(bucket, key, Optional.empty(), method, s3Headers)

  /**
   * Use this for a low level access to S3.
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param versionId optional versionId of source object
   * @param method the [[pekko.http.javadsl.model.HttpMethod HttpMethod]] to use when making the request
   * @param s3Headers any headers you want to add
   * @return a raw HTTP response from S3
   */
  def request(bucket: String,
      key: String,
      versionId: Optional[String],
      method: HttpMethod = HttpMethods.GET,
      s3Headers: S3Headers = S3Headers.empty): Source[HttpResponse, NotUsed] =
    S3Stream
      .request(S3Location(bucket, key),
        method.asInstanceOf[ScalaHttpMethod],
        versionId = Option(versionId.orElse(null)),
        s3Headers = s3Headers.headers)
      .map(v => v: HttpResponse)
      .asJava

  /**
   * Gets the metadata for a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @return A [[pekko.stream.javadsl.Source Source]] containing an [[java.util.Optional Optional]] that will be empty in case the object does not exist
   */
  def getObjectMetadata(bucket: String, key: String): Source[Optional[ObjectMetadata], NotUsed] =
    getObjectMetadata(bucket, key, S3Headers.empty)

  /**
   * Gets the metadata for a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param sse the server side encryption to use
   * @return A [[pekko.stream.javadsl.Source Source]] containing an [[java.util.Optional Optional]] that will be empty in case the object does not exist
   */
  def getObjectMetadata(bucket: String,
      key: String,
      sse: ServerSideEncryption): Source[Optional[ObjectMetadata], NotUsed] =
    getObjectMetadata(bucket, key, S3Headers.empty.withOptionalServerSideEncryption(Option(sse)))

  /**
   * Gets the metadata for a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param versionId optional versionId of source object
   * @param sse the server side encryption to use
   * @return A [[pekko.stream.javadsl.Source Source]] containing an [[java.util.Optional Optional]] that will be empty in case the object does not exist
   */
  def getObjectMetadata(bucket: String,
      key: String,
      versionId: Optional[String],
      sse: ServerSideEncryption): Source[Optional[ObjectMetadata], NotUsed] =
    getObjectMetadataWithHeaders(bucket, key, versionId, S3Headers.empty.withOptionalServerSideEncryption(Option(sse)))

  /**
   * Gets the metadata for a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param s3Headers any headers you want to add
   * @return A [[pekko.stream.javadsl.Source Source]] containing an [[java.util.Optional Optional]] that will be empty in case the object does not exist
   */
  def getObjectMetadata(bucket: String, key: String, s3Headers: S3Headers): Source[Optional[ObjectMetadata], NotUsed] =
    getObjectMetadataWithHeaders(bucket, key, Optional.empty(), s3Headers)

  /**
   * Gets the metadata for a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param versionId optional versionId of source object
   * @param s3Headers any headers you want to add
   * @return A [[pekko.stream.javadsl.Source Source]] containing an [[java.util.Optional Optional]] that will be empty in case the object does not exist
   */
  def getObjectMetadataWithHeaders(bucket: String,
      key: String,
      versionId: Optional[String],
      s3Headers: S3Headers): Source[Optional[ObjectMetadata], NotUsed] =
    S3Stream
      .getObjectMetadata(bucket, key, Option(versionId.orElse(null)), s3Headers)
      .map { opt =>
        Optional.ofNullable(opt.orNull)
      }
      .asJava

  /**
   * Deletes a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteObject(bucket: String, key: String): Source[Done, NotUsed] =
    deleteObject(bucket, key, Optional.empty(), S3Headers.empty)

  /**
   * Deletes a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param versionId optional version id of the object
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteObject(bucket: String, key: String, versionId: Optional[String]): Source[Done, NotUsed] =
    deleteObject(bucket, key, versionId, S3Headers.empty)

  /**
   * Deletes a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param versionId optional version id of the object
   * @param s3Headers any headers you want to add
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteObject(bucket: String,
      key: String,
      versionId: Optional[String],
      s3Headers: S3Headers): Source[Done, NotUsed] =
    S3Stream
      .deleteObject(S3Location(bucket, key), Option(versionId.orElse(null)), s3Headers)
      .map(_ => Done.getInstance())
      .asJava

  /**
   * Deletes all keys under the specified bucket
   *
   * @param bucket the s3 bucket name
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteObjectsByPrefix(bucket: String): Source[Done, NotUsed] =
    deleteObjectsByPrefix(bucket, Optional.empty(), deleteAllVersions = false, S3Headers.empty)

  /**
   * Deletes all keys under the specified bucket
   *
   * @param bucket the s3 bucket name
   * @param deleteAllVersions Whether to delete all object versions as well (applies to versioned buckets)
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteObjectsByPrefix(bucket: String, deleteAllVersions: Boolean): Source[Done, NotUsed] =
    deleteObjectsByPrefix(bucket, Optional.empty(), deleteAllVersions, S3Headers.empty)

  /**
   * Deletes all keys which have the given prefix under the specified bucket
   *
   * @param bucket the s3 bucket name
   * @param prefix optional s3 objects prefix
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteObjectsByPrefix(bucket: String, prefix: Optional[String]): Source[Done, NotUsed] =
    deleteObjectsByPrefix(bucket, prefix, S3Headers.empty)

  /**
   * Deletes all keys which have the given prefix under the specified bucket
   *
   * @param bucket the s3 bucket name
   * @param prefix optional s3 objects prefix
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteObjectsByPrefix(bucket: String,
      prefix: Optional[String],
      deleteAllVersions: Boolean): Source[Done, NotUsed] =
    deleteObjectsByPrefix(bucket, prefix, deleteAllVersions, S3Headers.empty)

  /**
   * Deletes all keys which have the given prefix under the specified bucket
   *
   * @param bucket the s3 bucket name
   * @param prefix optional s3 objects prefix
   * @param s3Headers any headers you want to add
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteObjectsByPrefix(bucket: String, prefix: Optional[String], s3Headers: S3Headers): Source[Done, NotUsed] =
    S3.deleteObjectsByPrefix(bucket, prefix, deleteAllVersions = false, s3Headers)

  /**
   * Deletes all keys which have the given prefix under the specified bucket
   *
   * @param bucket the s3 bucket name
   * @param prefix optional s3 objects prefix
   * @param s3Headers any headers you want to add
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteObjectsByPrefix(bucket: String,
      prefix: Optional[String],
      deleteAllVersions: Boolean,
      s3Headers: S3Headers): Source[Done, NotUsed] =
    S3Stream
      .deleteObjectsByPrefix(bucket, Option(prefix.orElse(null)), deleteAllVersions, s3Headers)
      .map(_ => Done.getInstance())
      .asJava

  /**
   * Deletes all S3 Objects within the given bucket
   *
   * @param bucket the s3 bucket name
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteBucketContents(bucket: String): Source[Done, NotUsed] =
    S3.deleteBucketContents(bucket, deleteAllVersions = false)

  /**
   * Deletes all S3 Objects within the given bucket
   *
   * @param bucket the s3 bucket name
   * @param deleteAllVersions Whether to delete all object versions as well (applies to versioned buckets)
   * @return A [[pekko.stream.javadsl.Source Source]] that will emit [[pekko.Done]] when operation is completed
   */
  def deleteBucketContents(bucket: String, deleteAllVersions: Boolean): Source[Done, NotUsed] =
    S3Stream
      .deleteObjectsByPrefix(bucket, None, deleteAllVersions, S3Headers.empty)
      .map(_ => Done.getInstance())
      .asJava

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any additional headers for the request
   * @return a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
      key: String,
      data: Source[ByteString, _],
      contentLength: Long,
      contentType: ContentType,
      s3Headers: S3Headers): Source[ObjectMetadata, NotUsed] =
    S3Stream
      .putObject(S3Location(bucket, key),
        contentType.asInstanceOf[ScalaContentType],
        data.asScala,
        contentLength,
        s3Headers)
      .asJava

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @return a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
      key: String,
      data: Source[ByteString, _],
      contentLength: Long,
      contentType: ContentType): Source[ObjectMetadata, NotUsed] =
    putObject(bucket, key, data, contentLength, contentType, S3Headers.empty.withCannedAcl(CannedAcl.Private))

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @return a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
      key: String,
      data: Source[ByteString, _],
      contentLength: Long): Source[ObjectMetadata, NotUsed] =
    putObject(bucket, key, data, contentLength, ContentTypes.APPLICATION_OCTET_STREAM)

  private def toJava[M](
      download: pekko.stream.scaladsl.Source[Option[
          (pekko.stream.scaladsl.Source[ByteString, M], ObjectMetadata)], NotUsed])
      : Source[Optional[JPair[Source[ByteString, M], ObjectMetadata]], NotUsed] =
    download.map {
      _.map { case (stream, meta) => JPair(stream.asJava, meta) }.toJava
    }.asJava

  /**
   * Downloads a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @return A [[pekko.japi.Pair]] with a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]], and a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]]
   */
  @deprecated("Use S3.getObject instead", "4.0.0")
  def download(bucket: String,
      key: String): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] =
    toJava(S3Stream.download(S3Location(bucket, key), None, None, S3Headers.empty))

  /**
   * Downloads a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param sse the server side encryption to use
   * @return A [[pekko.japi.Pair]] with a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]], and a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]]
   */
  @deprecated("Use S3.getObject instead", "4.0.0")
  def download(
      bucket: String,
      key: String,
      sse: ServerSideEncryption): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] =
    download(bucket, key, S3Headers.empty.withOptionalServerSideEncryption(Option(sse)))

  /**
   * Downloads a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @return A [[pekko.japi.Pair]] with a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]], and a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]]
   */
  @deprecated("Use S3.getObject instead", "4.0.0")
  def download(bucket: String,
      key: String,
      range: ByteRange): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] = {
    val scalaRange = range.asInstanceOf[ScalaByteRange]
    toJava(S3Stream.download(S3Location(bucket, key), Some(scalaRange), None, S3Headers.empty))
  }

  /**
   * Downloads a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param sse the server side encryption to use
   * @return A [[pekko.japi.Pair]] with a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]], and a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]]
   */
  @deprecated("Use S3.getObject instead", "4.0.0")
  def download(
      bucket: String,
      key: String,
      range: ByteRange,
      sse: ServerSideEncryption): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] =
    download(bucket, key, range, S3Headers.empty.withOptionalServerSideEncryption(Option(sse)))

  /**
   * Downloads a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param versionId optional version id of the object
   * @param sse the server side encryption to use
   * @return A [[pekko.japi.Pair]] with a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]], and a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]]
   */
  @deprecated("Use S3.getObject instead", "4.0.0")
  def download(
      bucket: String,
      key: String,
      range: ByteRange,
      versionId: Optional[String],
      sse: ServerSideEncryption): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] =
    download(bucket, key, range, versionId, S3Headers.empty.withOptionalServerSideEncryption(Option(sse)))

  /**
   * Downloads a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param s3Headers any headers you want to add
   * @return A [[pekko.japi.Pair]] with a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]], and a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]]
   */
  @deprecated("Use S3.getObject instead", "4.0.0")
  def download(
      bucket: String,
      key: String,
      s3Headers: S3Headers): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] =
    toJava(
      S3Stream.download(S3Location(bucket, key), None, None, s3Headers))

  /**
   * Downloads a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param s3Headers any headers you want to add
   * @return A [[pekko.japi.Pair]] with a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]], and a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]]
   */
  @deprecated("Use S3.getObject instead", "4.0.0")
  def download(
      bucket: String,
      key: String,
      range: ByteRange,
      s3Headers: S3Headers): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] = {
    val scalaRange = range.asInstanceOf[ScalaByteRange]
    toJava(
      S3Stream.download(S3Location(bucket, key), Some(scalaRange), None, s3Headers))
  }

  /**
   * Downloads a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param versionId optional version id of the object
   * @param s3Headers any headers you want to add
   * @return A [[pekko.japi.Pair]] with a [[pekko.stream.javadsl.Source Source]] of [[pekko.util.ByteString ByteString]], and a [[pekko.stream.javadsl.Source Source]] containing the [[ObjectMetadata]]
   */
  @deprecated("Use S3.getObject instead", "4.0.0")
  def download(
      bucket: String,
      key: String,
      range: ByteRange,
      versionId: Optional[String],
      s3Headers: S3Headers): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] = {
    val scalaRange = range.asInstanceOf[ScalaByteRange]
    toJava(
      S3Stream.download(S3Location(bucket, key), Option(scalaRange), Option(versionId.orElse(null)), s3Headers))
  }

  /**
   * Gets a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @return A [[pekko.stream.javadsl.Source]] containing the objects data as a [[pekko.util.ByteString]] along with a materialized value containing the
   *         [[pekko.stream.connectors.s3.ObjectMetadata]]
   */
  def getObject(bucket: String, key: String): Source[ByteString, CompletionStage[ObjectMetadata]] =
    new Source(S3Stream.getObject(S3Location(bucket, key), None, None, S3Headers.empty).toCompletionStage())

  /**
   * Gets a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param sse the server side encryption to use
   * @return A [[pekko.stream.javadsl.Source]] containing the objects data as a [[pekko.util.ByteString]] along with a materialized value containing the
   *         [[pekko.stream.connectors.s3.ObjectMetadata]]
   */
  def getObject(
      bucket: String,
      key: String,
      sse: ServerSideEncryption): Source[ByteString, CompletionStage[ObjectMetadata]] =
    getObject(bucket, key, S3Headers.empty.withOptionalServerSideEncryption(Option(sse)))

  /**
   * Gets a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @return A [[pekko.stream.javadsl.Source]] containing the objects data as a [[pekko.util.ByteString]] along with a materialized value containing the
   *         [[pekko.stream.connectors.s3.ObjectMetadata]]
   */
  def getObject(bucket: String, key: String, range: ByteRange): Source[ByteString, CompletionStage[ObjectMetadata]] = {
    val scalaRange = range.asInstanceOf[ScalaByteRange]
    new Source(S3Stream.getObject(S3Location(bucket, key), Some(scalaRange), None, S3Headers.empty).toCompletionStage())
  }

  /**
   * Gets a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param sse the server side encryption to use
   * @return A [[pekko.stream.javadsl.Source]] containing the objects data as a [[pekko.util.ByteString]] along with a materialized value containing the
   *         [[pekko.stream.connectors.s3.ObjectMetadata]]
   */
  def getObject(
      bucket: String,
      key: String,
      range: ByteRange,
      sse: ServerSideEncryption): Source[ByteString, CompletionStage[ObjectMetadata]] =
    getObject(bucket, key, range, S3Headers.empty.withOptionalServerSideEncryption(Option(sse)))

  /**
   * Gets a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param versionId optional version id of the object
   * @param sse the server side encryption to use
   * @return A [[pekko.stream.javadsl.Source]] containing the objects data as a [[pekko.util.ByteString]] along with a materialized value containing the
   *         [[pekko.stream.connectors.s3.ObjectMetadata]]
   */
  def getObject(
      bucket: String,
      key: String,
      range: ByteRange,
      versionId: Optional[String],
      sse: ServerSideEncryption): Source[ByteString, CompletionStage[ObjectMetadata]] =
    getObject(bucket, key, range, versionId, S3Headers.empty.withOptionalServerSideEncryption(Option(sse)))

  /**
   * Gets a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param s3Headers any headers you want to add
   * @return A [[pekko.stream.javadsl.Source]] containing the objects data as a [[pekko.util.ByteString]] along with a materialized value containing the
   *         [[pekko.stream.connectors.s3.ObjectMetadata]]
   */
  def getObject(
      bucket: String,
      key: String,
      s3Headers: S3Headers): Source[ByteString, CompletionStage[ObjectMetadata]] =
    new Source(S3Stream.getObject(S3Location(bucket, key), None, None, s3Headers).toCompletionStage())

  /**
   * Gets a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param s3Headers any headers you want to add
   * @return A [[pekko.stream.javadsl.Source]] containing the objects data as a [[pekko.util.ByteString]] along with a materialized value containing the
   *         [[pekko.stream.connectors.s3.ObjectMetadata]]
   */
  def getObject(
      bucket: String,
      key: String,
      range: ByteRange,
      s3Headers: S3Headers): Source[ByteString, CompletionStage[ObjectMetadata]] = {
    val scalaRange = range.asInstanceOf[ScalaByteRange]
    new Source(S3Stream.getObject(S3Location(bucket, key), Some(scalaRange), None, s3Headers).toCompletionStage())
  }

  /**
   * Gets a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[pekko.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param versionId optional version id of the object
   * @param s3Headers any headers you want to add
   * @return A [[pekko.stream.javadsl.Source]] containing the objects data as a [[pekko.util.ByteString]] along with a materialized value containing the
   *         [[pekko.stream.connectors.s3.ObjectMetadata]]
   */
  def getObject(
      bucket: String,
      key: String,
      range: ByteRange,
      versionId: Optional[String],
      s3Headers: S3Headers): Source[ByteString, CompletionStage[ObjectMetadata]] = {
    val scalaRange = range.asInstanceOf[ScalaByteRange]
    new Source(
      S3Stream
        .getObject(S3Location(bucket, key), Option(scalaRange), Option(versionId.orElse(null)), s3Headers)
        .toCompletionStage())
  }

  /**
   * Will return a list containing all of the buckets for the current AWS account
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBuckets.html
   * @return [[pekko.stream.javadsl.Source Source]] of [[ListBucketsResultContents]]
   */
  def listBuckets(): Source[ListBucketsResultContents, NotUsed] =
    listBuckets(S3Headers.empty)

  /**
   * Will return a list containing all of the buckets for the current AWS account
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBuckets.html
   * @return [[pekko.stream.javadsl.Source Source]] of [[ListBucketsResultContents]]
   */
  def listBuckets(s3Headers: S3Headers): Source[ListBucketsResultContents, NotUsed] =
    S3Stream
      .listBuckets(s3Headers)
      .asJava

  /**
   * Will return a source of object metadata for a given bucket with optional prefix using version 2 of the List Bucket API.
   * This will automatically page through all keys with the given parameters.
   *
   * The <code>org.apache.pekko.stream.connectors.s3.list-bucket-api-version</code> can be set to 1 to use the older API version 1
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html  (version 2 API)
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html (version 1 API)
   * @param bucket Which bucket that you list object metadata for
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @return Source of object metadata
   */
  def listBucket(bucket: String, prefix: Optional[String]): Source[ListBucketResultContents, NotUsed] =
    listBucket(bucket, prefix, S3Headers.empty)

  /**
   * Will return a source of object metadata for a given bucket with optional prefix using version 2 of the List Bucket API.
   * This will automatically page through all keys with the given parameters.
   *
   * The <code>org.apache.pekko.stream.connectors.s3.list-bucket-api-version</code> can be set to 1 to use the older API version 1
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html  (version 2 API)
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html (version 1 API)
   * @param bucket Which bucket that you list object metadata for
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @return Source of object metadata
   */
  def listBucket(bucket: String,
      prefix: Optional[String],
      s3Headers: S3Headers): Source[ListBucketResultContents, NotUsed] =
    S3Stream
      .listBucket(bucket, prefix.toScala, s3Headers)
      .asJava

  /**
   * Will return a source of object metadata for a given bucket with delimiter and optional prefix using version 2 of the List Bucket API.
   * This will automatically page through all keys with the given parameters.
   *
   * The <code>org.apache.pekko.stream.connectors.s3.list-bucket-api-version</code> can be set to 1 to use the older API version 1
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html  (version 2 API)
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html (version 1 API)
   * @param bucket    Which bucket that you list object metadata for
   * @param delimiter Delimiter to use for listing only one level of hierarchy
   * @param prefix    Prefix of the keys you want to list under passed bucket
   * @return Source of object metadata
   */
  def listBucket(bucket: String,
      delimiter: String,
      prefix: Optional[String]): Source[ListBucketResultContents, NotUsed] =
    scaladsl.S3
      .listBucket(bucket, delimiter, prefix.toScala)
      .asJava

  /**
   * Will return a source of object metadata for a given bucket with delimiter and optional prefix using version 2 of the List Bucket API.
   * This will automatically page through all keys with the given parameters.
   *
   * The <code>org.apache.pekko.stream.connectors.s3.list-bucket-api-version</code> can be set to 1 to use the older API version 1
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html  (version 2 API)
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html (version 1 API)
   * @param bucket    Which bucket that you list object metadata for
   * @param delimiter Delimiter to use for listing only one level of hierarchy
   * @param prefix    Prefix of the keys you want to list under passed bucket
   * @param s3Headers any headers you want to add
   * @return Source of object metadata
   */
  def listBucket(bucket: String,
      delimiter: String,
      prefix: Optional[String],
      s3Headers: S3Headers): Source[ListBucketResultContents, NotUsed] =
    scaladsl.S3
      .listBucket(bucket, delimiter, prefix.toScala, s3Headers)
      .asJava

  /**
   * Will return a source of object metadata and common prefixes for a given bucket and delimiter with optional prefix using version 2 of the List Bucket API.
   * This will automatically page through all keys with the given parameters.
   *
   * The `pekko.connectors.s3.list-bucket-api-version` can be set to 1 to use the older API version 1
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html  (version 2 API)
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html (version 1 API)
   * @see https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingKeysHierarchy.html (prefix and delimiter documentation)
   * @param bucket    Which bucket that you list object metadata for
   * @param delimiter Delimiter to use for listing only one level of hierarchy
   * @param prefix    Prefix of the keys you want to list under passed bucket
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.scaladsl.Source Source]] of [[pekko.japi.Pair Pair]] of ([[java.util.List List]] of [[pekko.stream.connectors.s3.ListBucketResultContents ListBucketResultContents]], [[java.util.List List]] of [[pekko.stream.connectors.s3.ListBucketResultCommonPrefixes ListBucketResultCommonPrefixes]]
   */
  def listBucketAndCommonPrefixes(
      bucket: String,
      delimiter: String,
      prefix: Optional[String],
      s3Headers: S3Headers): Source[pekko.japi.Pair[java.util.List[ListBucketResultContents], java.util.List[
      ListBucketResultCommonPrefixes]], NotUsed] =
    S3Stream
      .listBucketAndCommonPrefixes(bucket, delimiter, prefix.toScala, s3Headers)
      .map {
        case (contents, commonPrefixes) => pekko.japi.Pair(contents.asJava, commonPrefixes.asJava)
      }
      .asJava

  /**
   * Will return in progress or aborted multipart uploads. This will automatically page through all keys with the given parameters.
   *
   * @param bucket Which bucket that you list in-progress multipart uploads for
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @return [[pekko.stream.scaladsl.Source Source]] of [[ListMultipartUploadResultUploads]]
   */
  def listMultipartUpload(bucket: String, prefix: Optional[String]): Source[ListMultipartUploadResultUploads, NotUsed] =
    listMultipartUpload(bucket, prefix, S3Headers.empty)

  /**
   * Will return in progress or aborted multipart uploads. This will automatically page through all keys with the given parameters.
   *
   * @param bucket Which bucket that you list in-progress multipart uploads for
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.scaladsl.Source Source]] of [[ListMultipartUploadResultUploads]]
   */
  def listMultipartUpload(bucket: String,
      prefix: Optional[String],
      s3Headers: S3Headers): Source[ListMultipartUploadResultUploads, NotUsed] =
    scaladsl.S3.listMultipartUpload(bucket, prefix.toScala, s3Headers).asJava

  /**
   * Will return in progress or aborted multipart uploads with optional prefix and delimiter. This will automatically page through all keys with the given parameters.
   *
   * @param bucket Which bucket that you list in-progress multipart uploads for
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @param delimiter Delimiter to use for listing only one level of hierarchy
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.scaladsl.Source Source]] of [[pekko.japi.Pair Pair]] of ([[java.util.List List]] of [[pekko.stream.connectors.s3.ListMultipartUploadResultUploads ListMultipartUploadResultUploads]], [[java.util.List List]] of [[pekko.stream.connectors.s3.CommonPrefixes CommonPrefixes]])
   */
  def listMultipartUploadAndCommonPrefixes(
      bucket: String,
      delimiter: String,
      prefix: Optional[String],
      s3Headers: S3Headers = S3Headers.empty): Source[pekko.japi.Pair[java.util.List[ListMultipartUploadResultUploads],
    java.util.List[CommonPrefixes]], NotUsed] =
    S3Stream
      .listMultipartUploadAndCommonPrefixes(bucket, delimiter, prefix.toScala, s3Headers)
      .map {
        case (uploads, commonPrefixes) => pekko.japi.Pair(uploads.asJava, commonPrefixes.asJava)
      }
      .asJava

  /**
   * List uploaded parts for a specific upload. This will automatically page through all keys with the given parameters.
   *
   * @param bucket Under which bucket the upload parts are contained
   * @param key They key where the parts were uploaded to
   * @param uploadId Unique identifier of the upload for which you want to list the uploaded parts
   * @return [[pekko.stream.scaladsl.Source Source]] of [[ListPartsResultParts]]
   */
  def listParts(bucket: String, key: String, uploadId: String): Source[ListPartsResultParts, NotUsed] =
    listParts(bucket, key, uploadId, S3Headers.empty)

  /**
   * List uploaded parts for a specific upload. This will automatically page through all keys with the given parameters.
   *
   * @param bucket Under which bucket the upload parts are contained
   * @param key They key where the parts were uploaded to
   * @param uploadId Unique identifier of the upload for which you want to list the uploaded parts
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.scaladsl.Source Source]] of [[ListPartsResultParts]]
   */
  def listParts(bucket: String,
      key: String,
      uploadId: String,
      s3Headers: S3Headers): Source[ListPartsResultParts, NotUsed] =
    scaladsl.S3.listParts(bucket, key, uploadId, s3Headers).asJava

  /**
   * List all versioned objects for a bucket with optional prefix. This will automatically page through all keys with the given parameters.
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectVersions.html
   * @param bucket Which bucket that you list object versions for
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @return [[pekko.stream.scaladsl.Source Source]] of [[pekko.japi.Pair Pair]] of ([[java.util.List List]] of [[pekko.stream.connectors.s3.ListObjectVersionsResultVersions ListObjectVersionsResultVersions]], [[java.util.List List]] of [[pekko.stream.connectors.s3.DeleteMarkers DeleteMarkers]])
   */
  def listObjectVersions(
      bucket: String,
      prefix: Optional[String])
      : Source[pekko.japi.Pair[java.util.List[ListObjectVersionsResultVersions], java.util.List[
          DeleteMarkers]], NotUsed] =
    S3Stream
      .listObjectVersions(bucket, prefix.toScala, S3Headers.empty)
      .map {
        case (versions, markers) => pekko.japi.Pair(versions.asJava, markers.asJava)
      }
      .asJava

  /**
   * List all versioned objects for a bucket with optional prefix. This will automatically page through all keys with the given parameters.
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectVersions.html
   * @param bucket Which bucket that you list object versions for
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.scaladsl.Source Source]] of [[pekko.japi.Pair Pair]] of ([[java.util.List List]] of [[pekko.stream.connectors.s3.ListObjectVersionsResultVersions ListObjectVersionsResultVersions]], [[java.util.List List]] of [[pekko.stream.connectors.s3.DeleteMarkers DeleteMarkers]])
   */
  def listObjectVersions(
      bucket: String,
      prefix: Optional[String],
      s3Headers: S3Headers): Source[pekko.japi.Pair[java.util.List[ListObjectVersionsResultVersions], java.util.List[
      DeleteMarkers]], NotUsed] =
    S3Stream
      .listObjectVersions(bucket, prefix.toScala, s3Headers)
      .map {
        case (versions, markers) => pekko.japi.Pair(versions.asJava, markers.asJava)
      }
      .asJava

  /**
   * List all versioned objects for a bucket with optional prefix and delimiter. This will automatically page through all keys with the given parameters.
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectVersions.html
   * @param bucket Which bucket that you list object versions for
   * @param delimiter Delimiter to use for listing only one level of hierarchy
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.scaladsl.Source Source]] of [[pekko.japi.Pair Pair]] of ([[java.util.List List]] of [[pekko.stream.connectors.s3.ListObjectVersionsResultVersions ListObjectVersionsResultVersions]], [[java.util.List List]] of [[pekko.stream.connectors.s3.DeleteMarkers DeleteMarkers]])
   */
  def listObjectVersions(
      bucket: String,
      delimiter: String,
      prefix: Optional[String],
      s3Headers: S3Headers): Source[pekko.japi.Pair[java.util.List[ListObjectVersionsResultVersions], java.util.List[
      DeleteMarkers]], NotUsed] =
    S3Stream
      .listObjectVersionsAndCommonPrefixes(bucket, delimiter, prefix.toScala, s3Headers)
      .map {
        case (versions, markers, _) =>
          pekko.japi.Pair(versions.asJava, markers.asJava)
      }
      .asJava

  /**
   * List all versioned objects for a bucket with optional prefix and delimiter. This will automatically page through all keys with the given parameters.
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectVersions.html
   * @param bucket Which bucket that you list object versions for
   * @param delimiter Delimiter to use for listing only one level of hierarchy
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.scaladsl.Source Source]] of [[pekko.japi.tuple.Tuple3 Tuple3]] of ([[java.util.List List]] of [[pekko.stream.connectors.s3.ListObjectVersionsResultVersions ListObjectVersionsResultVersions]], [[java.util.List List]] of [[pekko.stream.connectors.s3.DeleteMarkers DeleteMarkers]], [[java.util.List List]] of [[pekko.stream.connectors.s3.CommonPrefixes CommonPrefixes]])
   */
  def listObjectVersionsAndCommonPrefixes(bucket: String,
      delimiter: String,
      prefix: Option[String],
      s3Headers: S3Headers): Source[pekko.japi.tuple.Tuple3[java.util.List[
      ListObjectVersionsResultVersions], java.util.List[DeleteMarkers], java.util.List[CommonPrefixes]], NotUsed] =
    S3Stream
      .listObjectVersionsAndCommonPrefixes(bucket, delimiter, prefix, s3Headers)
      .map {
        case (versions, markers, prefixes) =>
          pekko.japi.tuple.Tuple3(versions.asJava, markers.asJava, prefixes.asJava)
      }
      .asJava

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
      key: String,
      contentType: ContentType,
      s3Headers: S3Headers): Sink[ByteString, CompletionStage[MultipartUploadResult]] =
    S3Stream
      .multipartUpload(S3Location(bucket, key), contentType.asInstanceOf[ScalaContentType], s3Headers)
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
      key: String,
      contentType: ContentType): Sink[ByteString, CompletionStage[MultipartUploadResult]] =
    multipartUpload(bucket, key, contentType, S3Headers.empty.withCannedAcl(CannedAcl.Private))

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String, key: String): Sink[ByteString, CompletionStage[MultipartUploadResult]] =
    multipartUpload(bucket, key, ContentTypes.APPLICATION_OCTET_STREAM)

  /**
   * Uploads a S3 Object by making multiple requests. Unlike `multipartUpload`, this version allows you to pass in a
   * context (typically from a `SourceWithContext`/`FlowWithContext`) along with a chunkUploadSink that defines how to
   * act whenever a chunk is uploaded.
   *
   * Note that this version of resuming multipart upload ignores buffering
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @tparam C The Context that is passed along with the `ByteString`
   * @param chunkUploadSink A sink that's a callback which gets executed whenever an entire Chunk is uploaded to S3
   *                        (successfully or unsuccessfully). Since each chunk can contain more than one emitted element
   *                        from the original flow/source you get provided with the list of context's.
   *
   *                        The internal implementation uses `Flow.alsoTo` for `chunkUploadSink` which means that
   *                        backpressure is applied to the upload stream if `chunkUploadSink` is too slow, likewise any
   *                        failure will also be propagated to the upload stream. Sink Materialization is also shared
   *                        with the returned `Sink`.
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.japi.Pair Pair]] of ([[pekko.util.ByteString ByteString]] of [[C]])'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUploadWithContext[C](
      bucket: String,
      key: String,
      chunkUploadSink: Sink[JPair[UploadPartResponse, java.lang.Iterable[C]], NotUsed],
      contentType: ContentType,
      s3Headers: S3Headers): Sink[JPair[ByteString, C], CompletionStage[MultipartUploadResult]] =
    S3Stream
      .multipartUploadWithContext[C](
        S3Location(bucket, key),
        chunkUploadSink
          .contramap[(UploadPartResponse, scala.collection.immutable.Iterable[C])] {
            case (uploadPartResponse, iterable) =>
              JPair(uploadPartResponse, iterable.asJava)
          }
          .asScala,
        contentType.asInstanceOf[ScalaContentType],
        s3Headers)
      .contramap[JPair[ByteString, C]](_.toScala)
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * Uploads a S3 Object by making multiple requests. Unlike `multipartUpload`, this version allows you to pass in a
   * context (typically from a `SourceWithContext`/`FlowWithContext`) along with a chunkUploadSink that defines how to
   * act whenever a chunk is uploaded.
   *
   * Note that this version of resuming multipart upload ignores buffering
   *
   * @tparam C The Context that is passed along with the `ByteString`
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param chunkUploadSink A sink that's a callback which gets executed whenever an entire Chunk is uploaded to S3
   *                        (successfully or unsuccessfully). Since each chunk can contain more than one emitted element
   *                        from the original flow/source you get provided with the list of context's.
   *
   *                        The internal implementation uses `Flow.alsoTo` for `chunkUploadSink` which means that
   *                        backpressure is applied to the upload stream if `chunkUploadSink` is too slow, likewise any
   *                        failure will also be propagated to the upload stream. Sink Materialization is also shared
   *                        with the returned `Sink`.
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.japi.Pair Pair]] of ([[pekko.util.ByteString ByteString]] of [[C]])'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUploadWithContext[C](
      bucket: String,
      key: String,
      chunkUploadSink: Sink[JPair[UploadPartResponse, java.lang.Iterable[C]], NotUsed],
      contentType: ContentType): Sink[JPair[ByteString, C], CompletionStage[MultipartUploadResult]] =
    multipartUploadWithContext[C](bucket,
      key,
      chunkUploadSink,
      contentType,
      S3Headers.empty.withCannedAcl(CannedAcl.Private))

  /**
   * Uploads a S3 Object by making multiple requests. Unlike `multipartUpload`, this version allows you to pass in a
   * context (typically from a `SourceWithContext`/`FlowWithContext`) along with a chunkUploadSink that defines how to
   * act whenever a chunk is uploaded.
   *
   * Note that this version of resuming multipart upload ignores buffering
   *
   * @tparam C The Context that is passed along with the `ByteString`
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param chunkUploadSink A sink that's a callback which gets executed whenever an entire Chunk is uploaded to S3
   *                        (successfully or unsuccessfully). Since each chunk can contain more than one emitted element
   *                        from the original flow/source you get provided with the list of context's.
   *
   *                        The internal implementation uses `Flow.alsoTo` for `chunkUploadSink` which means that
   *                        backpressure is applied to the upload stream if `chunkUploadSink` is too slow, likewise any
   *                        failure will also be propagated to the upload stream. Sink Materialization is also shared
   *                        with the returned `Sink`.
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.japi.Pair Pair]] of ([[pekko.util.ByteString ByteString]] of [[C]])'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUploadWithContext[C](
      bucket: String,
      key: String,
      chunkUploadSink: Sink[JPair[UploadPartResponse, java.lang.Iterable[C]], NotUsed])
      : Sink[JPair[ByteString, C], CompletionStage[MultipartUploadResult]] =
    multipartUploadWithContext[C](bucket, key, chunkUploadSink, ContentTypes.APPLICATION_OCTET_STREAM)

  /**
   * Resumes from a previously aborted multipart upload by providing the uploadId and previous upload part identifiers
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param uploadId the upload that you want to resume
   * @param previousParts The previously uploaded parts ending just before when this upload will commence
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def resumeMultipartUpload(bucket: String,
      key: String,
      uploadId: String,
      previousParts: java.lang.Iterable[Part],
      contentType: ContentType,
      s3Headers: S3Headers): Sink[ByteString, CompletionStage[MultipartUploadResult]] = {
    S3Stream
      .resumeMultipartUpload(S3Location(bucket, key),
        uploadId,
        previousParts.asScala.toList,
        contentType.asInstanceOf[ScalaContentType],
        s3Headers)
      .mapMaterializedValue(_.asJava)
      .asJava
  }

  /**
   * Resumes from a previously aborted multipart upload by providing the uploadId and previous upload part identifiers
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param uploadId the upload that you want to resume
   * @param previousParts The previously uploaded parts ending just before when this upload will commence
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def resumeMultipartUpload(bucket: String,
      key: String,
      uploadId: String,
      previousParts: java.lang.Iterable[Part],
      contentType: ContentType): Sink[ByteString, CompletionStage[MultipartUploadResult]] =
    resumeMultipartUpload(bucket,
      key,
      uploadId,
      previousParts,
      contentType,
      S3Headers.empty.withCannedAcl(CannedAcl.Private))

  /**
   * Resumes from a previously aborted multipart upload by providing the uploadId and previous upload part identifiers
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param uploadId the upload that you want to resume
   * @param previousParts The previously uploaded parts ending just before when this upload will commence
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def resumeMultipartUpload(
      bucket: String,
      key: String,
      uploadId: String,
      previousParts: java.lang.Iterable[Part]): Sink[ByteString, CompletionStage[MultipartUploadResult]] =
    resumeMultipartUpload(bucket, key, uploadId, previousParts, ContentTypes.APPLICATION_OCTET_STREAM)

  /**
   * Resumes from a previously aborted multipart upload by providing the uploadId and previous upload part identifiers.
   * Unlike `resumeMultipartUpload`, this version allows you to pass in a context (typically from a
   * `SourceWithContext`/`FlowWithContext`) along with a chunkUploadSink that defines how to act whenever a chunk is
   * uploaded.
   *
   * Note that this version of resuming multipart upload ignores buffering
   *
   * @tparam C The Context that is passed along with the `ByteString`
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param uploadId the upload that you want to resume
   * @param previousParts The previously uploaded parts ending just before when this upload will commence
   * @param chunkUploadSink A sink that's a callback which gets executed whenever an entire Chunk is uploaded to S3
   *                        (successfully or unsuccessfully). Since each chunk can contain more than one emitted element
   *                        from the original flow/source you get provided with the list of context's.
   *
   *                        The internal implementation uses `Flow.alsoTo` for `chunkUploadSink` which means that
   *                        backpressure is applied to the upload stream if `chunkUploadSink` is too slow, likewise any
   *                        failure will also be propagated to the upload stream. Sink Materialization is also shared
   *                        with the returned `Sink`.
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.japi.Pair Pair]] of ([[pekko.util.ByteString ByteString]] of [[C]])'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def resumeMultipartUploadWithContext[C](
      bucket: String,
      key: String,
      uploadId: String,
      previousParts: java.lang.Iterable[Part],
      chunkUploadSink: Sink[JPair[UploadPartResponse, java.lang.Iterable[C]], NotUsed],
      contentType: ContentType,
      s3Headers: S3Headers): Sink[JPair[ByteString, C], CompletionStage[MultipartUploadResult]] = {
    S3Stream
      .resumeMultipartUploadWithContext[C](
        S3Location(bucket, key),
        uploadId,
        previousParts.asScala.toList,
        chunkUploadSink
          .contramap[(UploadPartResponse, scala.collection.immutable.Iterable[C])] {
            case (uploadPartResponse, iterable) =>
              JPair(uploadPartResponse, iterable.asJava)
          }
          .asScala,
        contentType.asInstanceOf[ScalaContentType],
        s3Headers)
      .contramap[JPair[ByteString, C]](_.toScala)
      .mapMaterializedValue(_.asJava)
      .asJava
  }

  /**
   * Resumes from a previously aborted multipart upload by providing the uploadId and previous upload part identifiers.
   * Unlike `resumeMultipartUpload`, this version allows you to pass in a context (typically from a
   * `SourceWithContext`/`FlowWithContext`) along with a chunkUploadSink that defines how to act whenever a chunk is
   * uploaded.
   *
   * Note that this version of resuming multipart upload ignores buffering
   *
   * @tparam C The Context that is passed along with the `ByteString`
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param uploadId the upload that you want to resume
   * @param previousParts The previously uploaded parts ending just before when this upload will commence
   * @param chunkUploadSink A sink that's a callback which gets executed whenever an entire Chunk is uploaded to S3
   *                        (successfully or unsuccessfully). Since each chunk can contain more than one emitted element
   *                        from the original flow/source you get provided with the list of context's.
   *
   *                        The internal implementation uses `Flow.alsoTo` for `chunkUploadSink` which means that
   *                        backpressure is applied to the upload stream if `chunkUploadSink` is too slow, likewise any
   *                        failure will also be propagated to the upload stream. Sink Materialization is also shared
   *                        with the returned `Sink`.
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.japi.Pair Pair]] of ([[pekko.util.ByteString ByteString]] of [[C]])'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def resumeMultipartUploadWithContext[C](
      bucket: String,
      key: String,
      uploadId: String,
      previousParts: java.lang.Iterable[Part],
      chunkUploadSink: Sink[JPair[UploadPartResponse, java.lang.Iterable[C]], NotUsed],
      contentType: ContentType): Sink[JPair[ByteString, C], CompletionStage[MultipartUploadResult]] =
    resumeMultipartUploadWithContext[C](bucket,
      key,
      uploadId,
      previousParts,
      chunkUploadSink,
      contentType,
      S3Headers.empty.withCannedAcl(CannedAcl.Private))

  /**
   * Resumes from a previously aborted multipart upload by providing the uploadId and previous upload part identifiers.
   * Unlike `resumeMultipartUpload`, this version allows you to pass in a context (typically from a
   * `SourceWithContext`/`FlowWithContext`) along with a chunkUploadSink that defines how to act whenever a chunk is
   * uploaded.
   *
   * Note that this version of resuming multipart upload ignores buffering
   *
   * @tparam C The Context that is passed along with the `ByteString`
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param uploadId the upload that you want to resume
   * @param previousParts The previously uploaded parts ending just before when this upload will commence
   * @param chunkUploadSink A sink that's a callback which gets executed whenever an entire Chunk is uploaded to S3
   *                        (successfully or unsuccessfully). Since each chunk can contain more than one emitted element
   *                        from the original flow/source you get provided with the list of context's.
   *
   *                        The internal implementation uses `Flow.alsoTo` for `chunkUploadSink` which means that
   *                        backpressure is applied to the upload stream if `chunkUploadSink` is too slow, likewise any
   *                        failure will also be propagated to the upload stream. Sink Materialization is also shared
   *                        with the returned `Sink`.
   * @return a [[pekko.stream.javadsl.Sink Sink]] that accepts [[pekko.japi.Pair Pair]] of ([[pekko.util.ByteString ByteString]] of [[C]])'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def resumeMultipartUploadWithContext[C](
      bucket: String,
      key: String,
      uploadId: String,
      previousParts: java.lang.Iterable[Part],
      chunkUploadSink: Sink[JPair[UploadPartResponse, java.lang.Iterable[C]], NotUsed])
      : Sink[JPair[ByteString, C], CompletionStage[MultipartUploadResult]] =
    resumeMultipartUploadWithContext[C](bucket,
      key,
      uploadId,
      previousParts,
      chunkUploadSink,
      ContentTypes.APPLICATION_OCTET_STREAM)

  /**
   * Complete a multipart upload with an already given list of parts
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html
   * @param bucket bucket the s3 bucket name
   * @param key the s3 object key
   * @param uploadId the upload that you want to complete
   * @param parts A list of all of the parts for the multipart upload
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[MultipartUploadResult]]
   */
  def completeMultipartUpload(bucket: String, key: String, uploadId: String, parts: java.lang.Iterable[Part])(
      implicit system: ClassicActorSystemProvider,
      attributes: Attributes = Attributes()): CompletionStage[MultipartUploadResult] =
    completeMultipartUpload(bucket, key, uploadId, parts, S3Headers.empty)

  /**
   * Complete a multipart upload with an already given list of parts
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html
   * @param bucket bucket the s3 bucket name
   * @param key the s3 object key
   * @param uploadId the upload that you want to complete
   * @param parts A list of all of the parts for the multipart upload
   * @param s3Headers any headers you want to add
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[MultipartUploadResult]]
   */
  def completeMultipartUpload(
      bucket: String,
      key: String,
      uploadId: String,
      parts: java.lang.Iterable[Part],
      s3Headers: S3Headers)(
      implicit system: ClassicActorSystemProvider, attributes: Attributes): CompletionStage[MultipartUploadResult] =
    S3Stream
      .completeMultipartUpload(S3Location(bucket, key), uploadId, parts.asScala.toList, s3Headers)(
        SystemMaterializer(system).materializer,
        attributes)
      .asJava

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @param sourceVersionId version id of source object, if the versioning is enabled in source bucket
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @return the [[pekko.stream.connectors.s3.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
      sourceKey: String,
      targetBucket: String,
      targetKey: String,
      sourceVersionId: Optional[String],
      contentType: ContentType,
      s3Headers: S3Headers): RunnableGraph[CompletionStage[MultipartUploadResult]] =
    RunnableGraph
      .fromGraph {
        S3Stream
          .multipartCopy(
            S3Location(sourceBucket, sourceKey),
            S3Location(targetBucket, targetKey),
            Option(sourceVersionId.orElse(null)),
            contentType.asInstanceOf[ScalaContentType],
            s3Headers)
      }
      .mapMaterializedValue(func(_.asJava))

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @param sourceVersionId version id of source object, if the versioning is enabled in source bucket
   * @param s3Headers any headers you want to add
   * @return the [[pekko.stream.connectors.s3.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
      sourceKey: String,
      targetBucket: String,
      targetKey: String,
      sourceVersionId: Optional[String],
      s3Headers: S3Headers): RunnableGraph[CompletionStage[MultipartUploadResult]] =
    multipartCopy(sourceBucket,
      sourceKey,
      targetBucket,
      targetKey,
      sourceVersionId,
      ContentTypes.APPLICATION_OCTET_STREAM,
      s3Headers)

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @param contentType an optional [[pekko.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @return the [[pekko.stream.connectors.s3.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
      sourceKey: String,
      targetBucket: String,
      targetKey: String,
      contentType: ContentType,
      s3Headers: S3Headers): RunnableGraph[CompletionStage[MultipartUploadResult]] =
    multipartCopy(sourceBucket, sourceKey, targetBucket, targetKey, Optional.empty(), contentType, s3Headers)

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @param s3Headers any headers you want to add
   * @return the [[pekko.stream.connectors.s3.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
      sourceKey: String,
      targetBucket: String,
      targetKey: String,
      s3Headers: S3Headers): RunnableGraph[CompletionStage[MultipartUploadResult]] =
    multipartCopy(sourceBucket, sourceKey, targetBucket, targetKey, ContentTypes.APPLICATION_OCTET_STREAM, s3Headers)

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @return the [[pekko.stream.connectors.s3.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
      sourceKey: String,
      targetBucket: String,
      targetKey: String): RunnableGraph[CompletionStage[MultipartUploadResult]] =
    multipartCopy(sourceBucket,
      sourceKey,
      targetBucket,
      targetKey,
      ContentTypes.APPLICATION_OCTET_STREAM,
      S3Headers.empty)

  /**
   * Create new bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html
   * @param bucketName bucket name
   * @param system actor system to run with
   * @param attributes attributes to run request with
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def makeBucket(bucketName: String,
      system: ClassicActorSystemProvider,
      attributes: Attributes): CompletionStage[Done] =
    makeBucket(bucketName, system, attributes, S3Headers.empty)

  /**
   * Create new bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html
   * @param bucketName bucket name
   * @param system actor system to run with
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def makeBucket(bucketName: String, system: ClassicActorSystemProvider): CompletionStage[Done] =
    makeBucket(bucketName, system, Attributes(), S3Headers.empty)

  /**
   * Create new bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html
   * @param bucketName bucket name
   * @param system the actor system which provides the materializer to run with
   * @param attributes attributes to run request with
   * @param s3Headers any headers you want to add
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def makeBucket(bucketName: String,
      system: ClassicActorSystemProvider,
      attributes: Attributes,
      s3Headers: S3Headers): CompletionStage[Done] =
    S3Stream.makeBucket(bucketName, s3Headers)(SystemMaterializer(system).materializer, attributes).asJava

  /**
   * Create new bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html
   * @param bucketName bucket name
   * @return [[pekko.stream.javadsl.Source Source]] of type [[Done]] as API doesn't return any additional information
   */
  def makeBucketSource(bucketName: String): Source[Done, NotUsed] =
    makeBucketSource(bucketName, S3Headers.empty)

  /**
   * Create new bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html
   * @param bucketName bucket name
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.javadsl.Source Source]] of type [[Done]] as API doesn't return any additional information
   */
  def makeBucketSource(bucketName: String, s3Headers: S3Headers): Source[Done, NotUsed] =
    S3Stream.makeBucketSource(bucketName, s3Headers).asJava

  /**
   * Delete bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html
   * @param bucketName   bucket name
   * @param system the actor system which provides the materializer to run with
   * @param attributes attributes to run request with
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def deleteBucket(bucketName: String,
      system: ClassicActorSystemProvider,
      attributes: Attributes): CompletionStage[Done] =
    deleteBucket(bucketName, system, attributes, S3Headers.empty)

  /**
   * Delete bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html
   * @param bucketName   bucket name
   * @param system the actor system which provides the materializer to run with
   * @param attributes attributes to run request with
   * @param s3Headers any headers you want to add
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def deleteBucket(bucketName: String,
      system: ClassicActorSystemProvider,
      attributes: Attributes,
      s3Headers: S3Headers): CompletionStage[Done] =
    S3Stream.deleteBucket(bucketName, s3Headers)(SystemMaterializer(system).materializer, attributes).asJava

  /**
   * Delete bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html
   * @param bucketName   bucket name
   * @param system the actor system which provides the materializer to run with
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def deleteBucket(bucketName: String, system: ClassicActorSystemProvider): CompletionStage[Done] =
    deleteBucket(bucketName, system, Attributes(), S3Headers.empty)

  /**
   * Delete bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html
   * @param bucketName   bucket name
   * @return [[pekko.stream.javadsl.Source Source]] of type [[Done]] as API doesn't return any additional information
   */
  def deleteBucketSource(bucketName: String): Source[Done, NotUsed] =
    deleteBucketSource(bucketName, S3Headers.empty)

  /**
   * Delete bucket with a given name
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html
   * @param bucketName   bucket name
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.javadsl.Source Source]] of type [[Done]] as API doesn't return any additional information
   */
  def deleteBucketSource(bucketName: String, s3Headers: S3Headers): Source[Done, NotUsed] =
    S3Stream.deleteBucketSource(bucketName, s3Headers).asJava

  /**
   * Checks whether the bucket exists and the user has rights to perform the `ListBucket` operation
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html
   * @param bucketName   bucket name
   * @param system the actor system which provides the materializer to run with
   * @param attributes attributes to run request with
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[BucketAccess]]
   */
  def checkIfBucketExists(bucketName: String,
      system: ClassicActorSystemProvider,
      attributes: Attributes): CompletionStage[BucketAccess] =
    checkIfBucketExists(bucketName, system, attributes, S3Headers.empty)

  /**
   * Checks whether the bucket exists and the user has rights to perform the `ListBucket` operation
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html
   * @param bucketName   bucket name
   * @param system the actor system which provides the materializer to run with
   * @param attributes attributes to run request with
   * @param s3Headers any headers you want to add
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[BucketAccess]]
   */
  def checkIfBucketExists(bucketName: String,
      system: ClassicActorSystemProvider,
      attributes: Attributes,
      s3Headers: S3Headers): CompletionStage[BucketAccess] =
    S3Stream.checkIfBucketExists(bucketName, s3Headers)(SystemMaterializer(system).materializer, attributes).asJava

  /**
   * Checks whether the bucket exits and user has rights to perform ListBucket operation
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html
   * @param bucketName   bucket name
   * @param system the actor system which provides the materializer to run with
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[BucketAccess]]
   */
  def checkIfBucketExists(bucketName: String, system: ClassicActorSystemProvider): CompletionStage[BucketAccess] =
    checkIfBucketExists(bucketName, system, Attributes(), S3Headers.empty)

  /**
   * Checks whether the bucket exits and user has rights to perform ListBucket operation
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html
   * @param bucketName   bucket name
   * @return [[pekko.stream.javadsl.Source Source]] of type [[BucketAccess]]
   */
  def checkIfBucketExistsSource(bucketName: String): Source[BucketAccess, NotUsed] =
    checkIfBucketExistsSource(bucketName, S3Headers.empty)

  /**
   * Checks whether the bucket exits and user has rights to perform ListBucket operation
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html
   * @param bucketName   bucket name
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.javadsl.Source Source]] of type [[BucketAccess]]
   */
  def checkIfBucketExistsSource(bucketName: String, s3Headers: S3Headers): Source[BucketAccess, NotUsed] =
    S3Stream.checkIfBucketExistsSource(bucketName, s3Headers).asJava

  /**
   * Delete all existing parts for a specific upload id
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html
   * @param bucketName Which bucket the upload is inside
   * @param key The key for the upload
   * @param uploadId Unique identifier of the upload
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def deleteUpload(bucketName: String, key: String, uploadId: String)(
      implicit system: ClassicActorSystemProvider,
      attributes: Attributes = Attributes()): CompletionStage[Done] =
    deleteUpload(bucketName, key, uploadId, S3Headers.empty)

  /**
   * Delete all existing parts for a specific upload
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html
   * @param bucketName Which bucket the upload is inside
   * @param key The key for the upload
   * @param uploadId Unique identifier of the upload
   * @param s3Headers any headers you want to add
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def deleteUpload(
      bucketName: String,
      key: String,
      uploadId: String,
      s3Headers: S3Headers)(
      implicit system: ClassicActorSystemProvider, attributes: Attributes): CompletionStage[Done] =
    S3Stream
      .deleteUpload(bucketName, key, uploadId, s3Headers)(SystemMaterializer(system).materializer, attributes)
      .asJava

  /**
   * Delete all existing parts for a specific upload
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html
   * @param bucketName Which bucket the upload is inside
   * @param key The key for the upload
   * @param uploadId Unique identifier of the upload
   * @return [[pekko.stream.javadsl.Source Source]] of type [[Done]] as API doesn't return any additional information
   */
  def deleteUploadSource(bucketName: String, key: String, uploadId: String): Source[Done, NotUsed] =
    deleteUploadSource(bucketName, key, uploadId, S3Headers.empty)

  /**
   * Delete all existing parts for a specific upload
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html
   *
   * @param bucketName Which bucket the upload is inside
   * @param key The key for the upload
   * @param uploadId Unique identifier of the upload
   * @param s3Headers any headers you want to add
   * @return [[pekko.stream.javadsl.Source Source]] of type [[Done]] as API doesn't return any additional information
   */
  def deleteUploadSource(bucketName: String,
      key: String,
      uploadId: String,
      s3Headers: S3Headers): Source[Done, NotUsed] =
    S3Stream.deleteUploadSource(bucketName, key, uploadId, s3Headers).asJava

  private def func[T, R](f: T => R) = new pekko.japi.function.Function[T, R] {
    override def apply(param: T): R = f(param)
  }

  /**
   * Sets the versioning state of an existing bucket.
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketVersioning.html
   * @param bucketName       Bucket name
   * @param bucketVersioning The state that you want to update
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def putBucketVersioning(bucketName: String, bucketVersioning: BucketVersioning)(
      implicit system: ClassicActorSystemProvider,
      attributes: Attributes = Attributes()): CompletionStage[Done] =
    putBucketVersioning(bucketName, bucketVersioning, S3Headers.empty)

  /**
   * Sets the versioning state of an existing bucket.
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketVersioning.html
   * @param bucketName       Bucket name
   * @param bucketVersioning The state that you want to update
   * @param s3Headers  any headers you want to add
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[Done]] as API doesn't return any additional information
   */
  def putBucketVersioning(
      bucketName: String,
      bucketVersioning: BucketVersioning,
      s3Headers: S3Headers)(
      implicit system: ClassicActorSystemProvider, attributes: Attributes): CompletionStage[Done] =
    S3Stream
      .putBucketVersioning(bucketName, bucketVersioning, s3Headers)(SystemMaterializer(system).materializer, attributes)
      .asJava

  /**
   * Sets the versioning state of an existing bucket.
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketVersioning.html
   * @param bucketName       Bucket name
   * @param bucketVersioning The state that you want to update
   * @return [[pekko.stream.javadsl.Source Source]] of type [[Done]] as API doesn't return any additional information
   */
  def putBucketVersioningSource(bucketName: String, bucketVersioning: BucketVersioning): Source[Done, NotUsed] =
    putBucketVersioningSource(bucketName, bucketVersioning, S3Headers.empty)

  /**
   * Sets the versioning state of an existing bucket.
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketVersioning.html
   * @param bucketName       Bucket name
   * @param bucketVersioning The state that you want to update
   * @param s3Headers  any headers you want to add
   * @return [[pekko.stream.javadsl.Source Source]] of type [[Done]] as API doesn't return any additional information
   */
  def putBucketVersioningSource(bucketName: String,
      bucketVersioning: BucketVersioning,
      s3Headers: S3Headers): Source[Done, NotUsed] =
    S3Stream.putBucketVersioningSource(bucketName, bucketVersioning, s3Headers).asJava

  /**
   * Gets the versioning of an existing bucket
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html
   * @param bucketName Bucket name
   * @param system     the actor system which provides the materializer to run with
   * @param attributes attributes to run request with
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[BucketVersioningResult]]
   */
  def getBucketVersioning(bucketName: String,
      system: ClassicActorSystemProvider,
      attributes: Attributes): CompletionStage[BucketVersioningResult] =
    getBucketVersioning(bucketName, system, attributes, S3Headers.empty)

  /**
   * Gets the versioning of an existing bucket
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html
   * @param bucketName Bucket name
   * @param system     the actor system which provides the materializer to run with
   * @param attributes attributes to run request with
   * @param s3Headers  any headers you want to add
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[BucketVersioningResult]]
   */
  def getBucketVersioning(bucketName: String,
      system: ClassicActorSystemProvider,
      attributes: Attributes,
      s3Headers: S3Headers): CompletionStage[BucketVersioningResult] =
    S3Stream.getBucketVersioning(bucketName, s3Headers)(SystemMaterializer(system).materializer, attributes).asJava

  /**
   * Gets the versioning of an existing bucket
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html
   * @param bucketName Bucket name
   * @param system     the actor system which provides the materializer to run with
   * @return [[java.util.concurrent.CompletionStage CompletionStage]] of type [[BucketVersioningResult]]
   */
  def getBucketVersioning(
      bucketName: String, system: ClassicActorSystemProvider): CompletionStage[BucketVersioningResult] =
    getBucketVersioning(bucketName, system, Attributes(), S3Headers.empty)

  /**
   * Gets the versioning of an existing bucket
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html
   * @param bucketName Bucket name
   * @return [[pekko.stream.javadsl.Source Source]] of type [[BucketVersioningResult]]
   */
  def getBucketVersioningSource(bucketName: String): Source[BucketVersioningResult, NotUsed] =
    getBucketVersioningSource(bucketName, S3Headers.empty)

  /**
   * Gets the versioning of an existing bucket
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketVersioning.html
   * @param bucketName Bucket name
   * @param s3Headers  any headers you want to add
   * @return [[pekko.stream.javadsl.Source Source]] of type [[BucketVersioningResult]]
   */
  def getBucketVersioningSource(bucketName: String, s3Headers: S3Headers): Source[BucketVersioningResult, NotUsed] =
    S3Stream.getBucketVersioningSource(bucketName, s3Headers).asJava

}
