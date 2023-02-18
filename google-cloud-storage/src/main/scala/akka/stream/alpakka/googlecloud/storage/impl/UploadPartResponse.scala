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

package akka.stream.alpakka.googlecloud.storage.impl

import akka.stream.alpakka.googlecloud.storage.StorageObject
import akka.annotation.InternalApi

@InternalApi
private[impl] sealed trait UploadPartResponse

@InternalApi
private[impl] final case class SuccessfulUploadPart(multiPartUpload: MultiPartUpload, index: Int)
    extends UploadPartResponse
@InternalApi
private[impl] final case class FailedUploadPart(multiPartUpload: MultiPartUpload, index: Int, exception: Throwable)
    extends UploadPartResponse
@InternalApi
private[impl] final case class SuccessfulUpload(multiPartUpload: MultiPartUpload,
    index: Int,
    storageObject: StorageObject)
    extends UploadPartResponse
