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

package docs.javadsl;

import org.apache.pekko.actor.ActorSystem;

// #imports
import org.apache.pekko.stream.connectors.huawei.pushkit.*;
import org.apache.pekko.stream.connectors.huawei.pushkit.javadsl.HmsPushKit;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.AndroidConfig;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.AndroidNotification;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.BasicNotification;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.ClickAction;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.ErrorResponse;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.PushKitNotification;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.PushKitResponse;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.Response;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.Tokens;

// #imports
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import scala.collection.immutable.Set;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class PushKitExamples {

  public static void example() {
    ActorSystem system = ActorSystem.create();

    // #simple-send
    HmsSettings config = HmsSettings.create(system);
    PushKitNotification notification =
        PushKitNotification.fromJava()
            .withNotification(BasicNotification.fromJava().withTitle("title").withBody("body"))
            .withAndroidConfig(
                AndroidConfig.fromJava()
                    .withNotification(
                        AndroidNotification.fromJava()
                            .withClickAction(ClickAction.fromJava().withType(3))))
            .withTarget(new Tokens(new Set.Set1<>("token").toSeq()));

    Source.single(notification).runWith(HmsPushKit.fireAndForget(config), system);
    // #simple-send

    // #asFlow-send
    CompletionStage<List<Response>> result =
        Source.single(notification)
            .via(HmsPushKit.send(config))
            .map(
                res -> {
                  if (!res.isFailure()) {
                    PushKitResponse response = (PushKitResponse) res;
                    System.out.println("Response " + response);
                  } else {
                    ErrorResponse response = (ErrorResponse) res;
                    System.out.println("Send error " + response);
                  }
                  return res;
                })
            .runWith(Sink.seq(), system);
    // #asFlow-send
  }
}
