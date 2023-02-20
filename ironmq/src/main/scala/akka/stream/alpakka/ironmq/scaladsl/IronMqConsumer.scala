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

package akka.stream.alpakka.ironmq.scaladsl

import akka.NotUsed
import akka.dispatch.ExecutionContexts
import akka.stream.alpakka.ironmq._
import akka.stream.alpakka.ironmq.impl.IronMqPullStage
import akka.stream.scaladsl._

object IronMqConsumer {

  def atMostOnceSource(queueName: String, settings: IronMqSettings): Source[Message, NotUsed] =
    Source.fromGraph(new IronMqPullStage(queueName, settings)).mapAsync(1) { cm =>
      cm.commit().map(_ => cm.message)(ExecutionContexts.parasitic)
    }

  def atLeastOnceSource[K, V](queueName: String, settings: IronMqSettings): Source[CommittableMessage, NotUsed] =
    Source.fromGraph(new IronMqPullStage(queueName, settings))

}
