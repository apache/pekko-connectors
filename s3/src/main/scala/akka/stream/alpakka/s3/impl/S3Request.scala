/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.s3.impl
import akka.annotation.InternalApi

/**
 * Internal Api
 */
@InternalApi private[s3] sealed trait S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object GetObject extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object HeadObject extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object PutObject extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object InitiateMultipartUpload extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object UploadPart extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object CopyPart extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object DeleteObject extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object ListBucket extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object MakeBucket extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object DeleteBucket extends S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] case object CheckBucket extends S3Request
