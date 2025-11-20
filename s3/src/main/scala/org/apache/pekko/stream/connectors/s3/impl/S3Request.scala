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

package org.apache.pekko.stream.connectors.s3.impl

import org.apache.pekko.annotation.InternalApi

/**
 * Internal Api
 */
@InternalApi private[s3] sealed trait S3Request

/**
 * Internal Api
 */
@InternalApi private[s3] object S3Request {
  def fromString(str: String): Option[S3Request] = {
    str match {
      case "GetObject"               => Some(GetObject)
      case "HeadObject"              => Some(HeadObject)
      case "PutObject"               => Some(PutObject)
      case "InitiateMultipartUpload" => Some(InitiateMultipartUpload)
      case "UploadPart"              => Some(UploadPart)
      case "CopyPart"                => Some(CopyPart)
      case "DeleteObject"            => Some(DeleteObject)
      case "ListBucket"              => Some(ListBucket)
      case "MakeBucket"              => Some(MakeBucket)
      case "DeleteBucket"            => Some(DeleteBucket)
      case "CheckBucket"             => Some(CheckBucket)
      case "PutBucketVersioning"     => Some(PutBucketVersioning)
      case "GetBucketVersioning"     => Some(GetBucketVersioning)
      case _                         => None
    }
  }

  val allRequests: List[S3Request] = List(
    GetObject,
    HeadObject,
    PutObject,
    InitiateMultipartUpload,
    UploadPart,
    CopyPart,
    DeleteObject,
    ListBucket,
    MakeBucket,
    DeleteBucket,
    CheckBucket,
    PutBucketVersioning,
    GetBucketVersioning
  )
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object GetObject extends S3Request {
  override def toString() = "GetObject"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object HeadObject extends S3Request {
  override def toString() = "HeadObject"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object PutObject extends S3Request {
  override def toString() = "PutObject"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object InitiateMultipartUpload extends S3Request {
  override def toString() = "InitiateMultipartUpload"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object UploadPart extends S3Request {
  override def toString() = "UploadPart"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object CopyPart extends S3Request {
  override def toString() = "CopyPart"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object DeleteObject extends S3Request {
  override def toString() = "DeleteObject"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object ListBucket extends S3Request {
  override def toString() = "ListBucket"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object MakeBucket extends S3Request {
  override def toString() = "MakeBucket"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object DeleteBucket extends S3Request {
  override def toString() = "DeleteBucket"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object CheckBucket extends S3Request {
  override def toString() = "CheckBucket"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object PutBucketVersioning extends S3Request {
  override def toString() = "PutBucketVersioning"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object GetBucketVersioning extends S3Request {
  override def toString() = "GetBucketVersioning"
}
