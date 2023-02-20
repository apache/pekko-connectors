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

package akka.stream.alpakka.huawei.pushkit.javadsl

import akka.stream.alpakka.huawei.pushkit._
import akka.stream.alpakka.huawei.pushkit.impl.PushKitFlows
import akka.stream.alpakka.huawei.pushkit.models.{ PushKitNotification, Response }
import akka.stream.javadsl
import akka.{ Done, NotUsed }

import java.util.concurrent.CompletionStage

object HmsPushKit {

  def send(conf: HmsSettings): javadsl.Flow[PushKitNotification, Response, NotUsed] =
    PushKitFlows.pushKit(conf).asJava

  def fireAndForget(conf: HmsSettings): javadsl.Sink[PushKitNotification, CompletionStage[Done]] =
    send(conf)
      .toMat(javadsl.Sink.ignore(), javadsl.Keep.right[NotUsed, CompletionStage[Done]])

}
