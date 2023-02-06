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

package org.apache.pekko.stream.connectors.google.firebase.fcm.v1.scaladsl

import org.apache.pekko.stream.connectors.google.firebase.fcm.FcmSettings
import org.apache.pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmFlows
import org.apache.pekko.stream.connectors.google.firebase.fcm.v1.models.{ FcmNotification, FcmResponse }
import org.apache.pekko.stream.scaladsl.{ Flow, Keep, Sink }
import org.apache.pekko.{ Done, NotUsed }

import scala.concurrent.Future

object GoogleFcm {

  def sendWithPassThrough[T](conf: FcmSettings): Flow[(FcmNotification, T), (FcmResponse, T), NotUsed] =
    FcmFlows.fcmWithData[T](conf)

  def send(conf: FcmSettings): Flow[FcmNotification, FcmResponse, NotUsed] =
    FcmFlows.fcm(conf)

  def fireAndForget(conf: FcmSettings): Sink[FcmNotification, Future[Done]] =
    FcmFlows.fcm(conf).toMat(Sink.ignore)(Keep.right)

}
