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

package org.apache.pekko.stream.connectors.google.firebase.fcm.scaladsl

import org.apache.pekko
import pekko.stream.connectors.google.firebase.fcm.impl.FcmFlows
import pekko.stream.connectors.google.firebase.fcm.{ FcmNotification, FcmResponse, FcmSettings }
import pekko.stream.scaladsl.{ Flow, Keep, Sink }
import pekko.{ Done, NotUsed }

import scala.concurrent.Future

object GoogleFcm {

  /** Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.scaladsl.GoogleFcm */
  @deprecated("org.apache.pekko.stream.connectors.google.firebase.fcm.v1.scaladsl.GoogleFcm", "Alpakka 3.0.2")
  @Deprecated
  def sendWithPassThrough[T](conf: FcmSettings): Flow[(FcmNotification, T), (FcmResponse, T), NotUsed] =
    FcmFlows.fcmWithData[T](conf)

  /** Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.scaladsl.GoogleFcm */
  @deprecated("org.apache.pekko.stream.connectors.google.firebase.fcm.v1.scaladsl.GoogleFcm", "Alpakka 3.0.2")
  @Deprecated
  def send(conf: FcmSettings): Flow[FcmNotification, FcmResponse, NotUsed] =
    FcmFlows.fcm(conf)

  /** Use org.apache.pekko.stream.connectors.google.firebase.fcm.v1.scaladsl.GoogleFcm */
  @deprecated("org.apache.pekko.stream.connectors.google.firebase.fcm.v1.scaladsl.GoogleFcm", "Alpakka 3.0.2")
  @Deprecated
  def fireAndForget(conf: FcmSettings): Sink[FcmNotification, Future[Done]] =
    FcmFlows.fcm(conf).toMat(Sink.ignore)(Keep.right)

}
