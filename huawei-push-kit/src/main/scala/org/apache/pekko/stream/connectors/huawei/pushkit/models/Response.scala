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

package org.apache.pekko.stream.connectors.huawei.pushkit.models

sealed trait Response {
  def isFailure: Boolean
}

final case class PushKitResponse(code: String, msg: String, requestId: String) extends Response {
  val isFailure = false
  def getCode: String = code
  def getMsg: String = msg
  def getRequestId: String = requestId

  def isSuccessSend: Boolean = "80000000".equals(code)
}

final case class ErrorResponse(rawError: String) extends Response {
  val isFailure = true
  def getRawError: String = rawError
}
