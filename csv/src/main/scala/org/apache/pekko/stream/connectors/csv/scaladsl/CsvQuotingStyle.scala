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

package org.apache.pekko.stream.connectors.csv.scaladsl

import org.apache.pekko.stream.connectors.csv.javadsl

sealed trait CsvQuotingStyle

/**
 * Select which fields to quote in CSV formatting.
 */
object CsvQuotingStyle {

  /** Quote only fields requiring quotes */
  case object Required extends CsvQuotingStyle

  /** Quote all fields */
  case object Always extends CsvQuotingStyle

  /** Java to Scala conversion helper */
  def asScala(qs: javadsl.CsvQuotingStyle): CsvQuotingStyle = qs match {
    case javadsl.CsvQuotingStyle.ALWAYS   => CsvQuotingStyle.Always
    case javadsl.CsvQuotingStyle.REQUIRED => CsvQuotingStyle.Required
  }

}
