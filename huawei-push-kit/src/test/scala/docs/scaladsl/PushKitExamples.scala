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

package docs.scaladsl

import org.apache.pekko.actor.ActorSystem
//#imports
import org.apache.pekko
import pekko.stream.connectors.huawei.pushkit._
import pekko.stream.connectors.huawei.pushkit.scaladsl.HmsPushKit
import pekko.stream.connectors.huawei.pushkit.models.AndroidConfig
import pekko.stream.connectors.huawei.pushkit.models.AndroidNotification
import pekko.stream.connectors.huawei.pushkit.models.BasicNotification
import pekko.stream.connectors.huawei.pushkit.models.ClickAction
import pekko.stream.connectors.huawei.pushkit.models.Condition
import pekko.stream.connectors.huawei.pushkit.models.ErrorResponse
import pekko.stream.connectors.huawei.pushkit.models.PushKitNotification
import pekko.stream.connectors.huawei.pushkit.models.PushKitResponse
import pekko.stream.connectors.huawei.pushkit.models.Response
import pekko.stream.connectors.huawei.pushkit.models.Tokens

//#imports
import pekko.stream.scaladsl.Source
import pekko.stream.scaladsl.Sink

import scala.collection.immutable
import scala.concurrent.Future

class PushKitExamples {

  implicit val system = ActorSystem()

  // #simple-send
  val config = HmsSettings()
  val notification: PushKitNotification =
    PushKitNotification.empty
      .withNotification(
        BasicNotification.empty
          .withTitle("title")
          .withBody("body"))
      .withAndroidConfig(
        AndroidConfig.empty
          .withNotification(
            AndroidNotification.empty
              .withClickAction(
                ClickAction.empty
                  .withType(3))))
      .withTarget(Tokens(Set[String]("token").toSeq))

  Source
    .single(notification)
    .runWith(HmsPushKit.fireAndForget(config))
  // #simple-send

  // #asFlow-send
  val result1: Future[immutable.Seq[Response]] =
    Source
      .single(notification)
      .via(HmsPushKit.send(config))
      .map {
        case res @ PushKitResponse(code, msg, requestId) =>
          println(s"Response $res")
          res
        case res @ ErrorResponse(errorMessage) =>
          println(s"Send error $res")
          res
      }
      .runWith(Sink.seq)
  // #asFlow-send

  // #condition-builder
  import org.apache.pekko.stream.connectors.huawei.pushkit.models.Condition.{ Topic => CTopic }
  val condition = Condition(CTopic("TopicA") && (CTopic("TopicB") || (CTopic("TopicC") && !CTopic("TopicD"))))
  // #condition-builder
}
