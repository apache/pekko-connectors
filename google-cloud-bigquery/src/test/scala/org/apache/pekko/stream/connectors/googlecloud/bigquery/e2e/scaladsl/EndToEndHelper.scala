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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.e2e.scaladsl

import org.apache.pekko
import pekko.stream.connectors.googlecloud.bigquery.e2e.{ A, B, C }
import pekko.util.ByteString

import java.time.{ Instant, LocalDate, LocalDateTime, LocalTime }
import scala.util.Random

trait EndToEndHelper {

  private val rng = new Random(1234567890)

  val datasetId = f"e2e_dataset_${rng.nextInt(1000)}%03d"

  val tableId = f"e2e_table_${rng.nextInt(1000)}%03d"

  private def randomC(): C = C(
    BigDecimal(f"${rng.nextInt(100)}.${rng.nextInt(100)}%02d"),
    LocalDate.ofEpochDay(rng.nextInt(100 * 365)),
    LocalTime.ofSecondOfDay(rng.nextInt(60 * 60 * 24)),
    LocalDateTime.of(LocalDate.ofEpochDay(rng.nextInt(100 * 365)), LocalTime.ofSecondOfDay(rng.nextInt(60 * 60 * 24))),
    Instant.ofEpochSecond(rng.nextInt()))

  private def randomB(): B = B(
    if (rng.nextBoolean()) Some(rng.nextString(rng.nextInt(64))) else None,
    ByteString {
      val bytes = new Array[Byte](rng.nextInt(64))
      rng.nextBytes(bytes)
      bytes
    },
    Seq.fill(rng.nextInt(16))(randomC()))

  private def randomA(): A = A(
    rng.nextInt(),
    rng.nextLong(),
    rng.nextFloat(),
    rng.nextDouble(),
    rng.nextString(rng.nextInt(64)),
    rng.nextBoolean(),
    randomB())

  val rows = List.fill(10)(randomA())

}
