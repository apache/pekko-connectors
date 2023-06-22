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

package org.apache.pekko.stream.connectors.google.firebase.fcm.v1.models

sealed trait FcmResponse {
  def isFailure: Boolean
  def isSuccess: Boolean
}

final case class FcmSuccessResponse(name: String) extends FcmResponse {
  val isFailure = false
  val isSuccess = true
  def getName: String = name
}

final case class FcmErrorResponse(rawError: String) extends FcmResponse {
  val isFailure = true
  val isSuccess = false
  def getRawError: String = rawError
}
