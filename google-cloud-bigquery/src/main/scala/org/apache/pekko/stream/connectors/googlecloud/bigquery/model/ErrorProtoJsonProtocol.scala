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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.model

import org.apache.pekko
import pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray.BigQueryRestJsonProtocol._
import pekko.util.OptionConverters._
import com.fasterxml.jackson.annotation.{ JsonCreator, JsonProperty }
import spray.json.JsonFormat

import java.util

import scala.annotation.nowarn

/**
 * ErrorProto model
 * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/ErrorProto BigQuery reference]]
 *
 * @param reason a short error code that summarizes the error
 * @param location specifies where the error occurred, if present
 * @param message A human-readable description of the error
 */
final case class ErrorProto private (reason: Option[String], location: Option[String], message: Option[String]) {

  @nowarn("msg=never used")
  @JsonCreator
  private def this(@JsonProperty(value = "reason") reason: String,
      @JsonProperty("location") location: String,
      @JsonProperty(value = "message") message: String) =
    this(Option(reason), Option(location), Option(message))

  def getReason = reason.toJava
  def getLocation = location.toJava
  def getMessage = message.toJava

  def withReason(reason: Option[String]) =
    copy(reason = reason)
  def withReason(reason: util.Optional[String]) =
    copy(reason = reason.toScala)

  def withLocation(location: Option[String]) =
    copy(location = location)
  def withLocation(location: util.Optional[String]) =
    copy(location = location.toScala)

  def withMessage(message: Option[String]) =
    copy(message = message)
  def withMessage(message: util.Optional[String]) =
    copy(message = message.toScala)
}

object ErrorProto {

  /**
   * Java API: ErrorProto model
   * @see [[https://cloud.google.com/bigquery/docs/reference/rest/v2/ErrorProto BigQuery reference]]
   *
   * @param reason a short error code that summarizes the error
   * @param location specifies where the error occurred, if present
   * @param message A human-readable description of the error
   * @return an [[ErrorProto]]
   */
  def create(reason: util.Optional[String], location: util.Optional[String], message: util.Optional[String]) =
    ErrorProto(reason.toScala, location.toScala, message.toScala)

  implicit val format: JsonFormat[ErrorProto] = jsonFormat3(apply)
}
