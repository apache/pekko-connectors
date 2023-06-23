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

package org.apache.pekko.stream.connectors.google.firebase.fcm.v1.javadsl

import org.apache.pekko
import pekko.japi.Pair
import pekko.stream.connectors.google.firebase.fcm.FcmSettings
import pekko.stream.connectors.google.firebase.fcm.v1.impl.FcmFlows
import pekko.stream.connectors.google.firebase.fcm.v1.models._
import pekko.stream.{ javadsl, scaladsl }
import pekko.{ Done, NotUsed }

import java.util.concurrent.CompletionStage

object GoogleFcm {

  def sendWithPassThrough[T](conf: FcmSettings): javadsl.Flow[Pair[FcmNotification, T], Pair[FcmResponse, T], NotUsed] =
    scaladsl
      .Flow[Pair[FcmNotification, T]]
      .map(_.toScala)
      .via(FcmFlows.fcmWithData[T](conf))
      .map(t => Pair(t._1, t._2))
      .asJava

  def send(conf: FcmSettings): javadsl.Flow[FcmNotification, FcmResponse, NotUsed] =
    FcmFlows.fcm(conf).asJava

  def fireAndForget(conf: FcmSettings): javadsl.Sink[FcmNotification, CompletionStage[Done]] =
    send(conf)
      .toMat(javadsl.Sink.ignore(), javadsl.Keep.right[NotUsed, CompletionStage[Done]])

}
