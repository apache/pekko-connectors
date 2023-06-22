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

package org.apache.pekko.stream.connectors.huawei.pushkit.scaladsl

import org.apache.pekko
import pekko.stream.connectors.huawei.pushkit._
import pekko.stream.connectors.huawei.pushkit.impl.PushKitFlows
import pekko.stream.connectors.huawei.pushkit.models.{ PushKitNotification, Response }
import pekko.stream.scaladsl.{ Flow, Keep, Sink }
import pekko.{ Done, NotUsed }

import scala.concurrent.Future

object HmsPushKit {

  def send(conf: HmsSettings): Flow[PushKitNotification, Response, NotUsed] =
    PushKitFlows.pushKit(conf)

  def fireAndForget(conf: HmsSettings): Sink[PushKitNotification, Future[Done]] =
    send(conf).toMat(Sink.ignore)(Keep.right)

}
