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
@InternalApi private[s3] sealed trait S3Request {
  def allowedHeaders: Set[String]
}

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

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html#API_GetObject_RequestSyntax GetObject Request Syntax]]
   * @return all valid headers for GetObject request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "If-Match",
    "If-Modified-Since",
    "If-None-Match",
    "If-Unmodified-Since",
    "Range",
    "x-amz-server-side-encryption-customer-algorithm",
    "x-amz-server-side-encryption-customer-key",
    "x-amz-server-side-encryption-customer-key-MD5",
    "x-amz-request-payer",
    "x-amz-expected-bucket-owner",
    "x-amz-checksum-mode"
  )

  override def toString() = "GetObject"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object HeadObject extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadObject.html#API_HeadObject_RequestSyntax HeadObject Request Syntax]]
   * @return all valid headers for HeadObject request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "If-Match",
    "If-Modified-Since",
    "If-None-Match",
    "If-Unmodified-Since",
    "Range",
    "x-amz-server-side-encryption-customer-algorithm",
    "x-amz-server-side-encryption-customer-key",
    "x-amz-server-side-encryption-customer-key-MD5",
    "x-amz-request-payer",
    "x-amz-expected-bucket-owner",
    "x-amz-checksum-mode"
  )

  override def toString() = "HeadObject"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object PutObject extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html#API_PutObject_RequestSyntax PutObject Request Syntax]]
   *
   * @return all valid headers for PutObject request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "x-amz-acl",
    "Cache-Control",
    "Content-Disposition",
    "Content-Encoding",
    "Content-Language",
    "Content-Length",
    "Content-MD5",
    "Content-Type",
    "x-amz-sdk-checksum-algorithm",
    "x-amz-checksum-crc32",
    "x-amz-checksum-crc32c",
    "x-amz-checksum-crc64nvme",
    "x-amz-checksum-sha1",
    "x-amz-checksum-sha256",
    "Expires",
    "If-Match",
    "If-None-Match",
    "x-amz-grant-full-control",
    "x-amz-grant-read",
    "x-amz-grant-read-acp",
    "x-amz-grant-write-acp",
    "x-amz-write-offset-bytes",
    "x-amz-server-side-encryption",
    "x-amz-storage-class",
    "x-amz-website-redirect-location",
    "x-amz-server-side-encryption-customer-algorithm",
    "x-amz-server-side-encryption-customer-key",
    "x-amz-server-side-encryption-customer-key-MD5",
    "x-amz-server-side-encryption-aws-kms-key-id",
    "x-amz-server-side-encryption-context",
    "x-amz-server-side-encryption-bucket-key-enabled",
    "x-amz-request-payer",
    "x-amz-tagging",
    "x-amz-object-lock-mode",
    "x-amz-object-lock-retain-until-date",
    "x-amz-object-lock-legal-hold",
    "x-amz-expected-bucket-owner"
  )

  override def toString() = "PutObject"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object InitiateMultipartUpload extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html#API_CreateMultipartUpload_RequestSyntax API_CreateMultipartUpload Request Syntax]]
   *
   * @return all valid headers for InitiateMultipartUpload request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "x-amz-acl",
    "Cache-Control",
    "Content-Disposition",
    "Content-Encoding",
    "Content-Language",
    "Content-Type",
    "Expires",
    "x-amz-grant-full-control",
    "x-amz-grant-read",
    "x-amz-grant-read-acp",
    "x-amz-grant-write-acp",
    "x-amz-server-side-encryption",
    "x-amz-storage-class",
    "x-amz-website-redirect-location",
    "x-amz-server-side-encryption-customer-algorithm",
    "x-amz-server-side-encryption-customer-key",
    "x-amz-server-side-encryption-customer-key-MD5",
    "x-amz-server-side-encryption-aws-kms-key-id",
    "x-amz-server-side-encryption-context",
    "x-amz-server-side-encryption-bucket-key-enabled",
    "x-amz-request-payer",
    "x-amz-tagging",
    "x-amz-object-lock-mode",
    "x-amz-object-lock-retain-until-date",
    "x-amz-object-lock-legal-hold",
    "x-amz-expected-bucket-owner",
    "x-amz-checksum-algorithm",
    "x-amz-checksum-type"
  )

  override def toString() = "InitiateMultipartUpload"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object UploadPart extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html#API_UploadPart_RequestSyntax UploadPart RequestSyntax ]]
   *
   * @return all valid headers for UploadPart request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "Content-Length",
    "Content-MD5",
    "x-amz-sdk-checksum-algorithm",
    "x-amz-checksum-crc32",
    "x-amz-checksum-crc32c",
    "x-amz-checksum-crc64nvme",
    "x-amz-checksum-sha1",
    "x-amz-checksum-sha256",
    "x-amz-server-side-encryption-customer-algorithm",
    "x-amz-server-side-encryption-customer-key",
    "x-amz-server-side-encryption-customer-key-MD5",
    "x-amz-request-payer",
    "x-amz-expected-bucket-owner"
  )

  override def toString() = "UploadPart"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object CopyPart extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html#API_UploadPartCopy_RequestSyntax UploadPartCopy Request Syntax]]
   *
   * @return all valid headers for CopyPart request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "x-amz-copy-source",
    "x-amz-copy-source-if-match",
    "x-amz-copy-source-if-modified-since",
    "x-amz-copy-source-if-none-match",
    "x-amz-copy-source-if-unmodified-since",
    "x-amz-copy-source-range",
    "x-amz-server-side-encryption-customer-algorithm",
    "x-amz-server-side-encryption-customer-key",
    "x-amz-server-side-encryption-customer-key-MD5",
    "x-amz-copy-source-server-side-encryption-customer-algorithm",
    "x-amz-copy-source-server-side-encryption-customer-key",
    "x-amz-copy-source-server-side-encryption-customer-key-MD5",
    "x-amz-request-payer",
    "x-amz-expected-bucket-owner",
    "x-amz-source-expected-bucket-owner"
  )

  override def toString() = "CopyPart"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object DeleteObject extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html#API_DeleteObject_RequestSyntax DeleteObject Request Syntax ]]
   *
   * @return all valid headers for DeleteObject request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "x-amz-mfa",
    "x-amz-request-payer",
    "x-amz-bypass-governance-retention",
    "x-amz-expected-bucket-owner",
    "If-Match",
    "x-amz-if-match-last-modified-time",
    "x-amz-if-match-size"
  )

  override def toString() = "DeleteObject"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object ListBucket extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBuckets.html#API_ListBuckets_RequestSyntax ListBuckets Request Syntax]]
   *
   * @return all valid headers for ListBucket request type
   */
  override def allowedHeaders: Set[String] = Set("Host")

  override def toString() = "ListBucket"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object MakeBucket extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html#API_CreateBucket_RequestSyntax CreateBucket Request Syntax]]
   *
   * @return all valid headers for MakeBucket request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "x-amz-acl",
    "x-amz-grant-full-control",
    "x-amz-grant-read",
    "x-amz-grant-read-acp",
    "x-amz-grant-write",
    "x-amz-grant-write-acp",
    "x-amz-bucket-object-lock-enabled",
    "x-amz-object-ownership"
  )

  override def toString() = "MakeBucket"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object DeleteBucket extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html#API_DeleteBucket_RequestSyntax DeleteBucket RequestSyntax]]
   *
   * @return all valid headers for DeleteBucket request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "x-amz-expected-bucket-owner"
  )

  override def toString() = "DeleteBucket"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object CheckBucket extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html#API_HeadBucket_RequestSyntax HeadBucket RequestSyntax]]
   *
   * @return all valid headers for CheckBucket request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "x-amz-expected-bucket-owner"
  )

  override def toString() = "CheckBucket"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object PutBucketVersioning extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_control_PutBucketVersioning.html#API_control_PutBucketVersioning_RequestSyntax PutBucketVersioning Request Syntax]]
   *
   * @return all valid headers for PutBucketVersioning request type
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "Content-MD5",
    "x-amz-sdk-checksum-algorithm",
    "x-amz-mfa",
    "x-amz-expected-bucket-owner"
  )

  override def toString() = "PutBucketVersioning"
}

/**
 * Internal Api
 */
@InternalApi private[s3] case object GetBucketVersioning extends S3Request {

  /**
   * See [[https://docs.aws.amazon.com/AmazonS3/latest/API/API_control_GetBucketVersioning.html#API_control_GetBucketVersioning_RequestSyntax GetBucketVersioning Request Syntax]]
   *
   * @return
   */
  override def allowedHeaders: Set[String] = Set(
    "Host",
    "x-amz-expected-bucket-owner"
  )

  override def toString() = "GetBucketVersioning"
}
