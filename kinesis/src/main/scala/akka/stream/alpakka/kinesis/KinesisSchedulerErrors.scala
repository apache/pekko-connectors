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

package akka.stream.alpakka.kinesis

object KinesisSchedulerErrors {

  sealed class KinesisSchedulerError(err: Throwable) extends Throwable(err)
  final case class SchedulerUnexpectedShutdown(cause: Throwable) extends KinesisSchedulerError(cause)

}
