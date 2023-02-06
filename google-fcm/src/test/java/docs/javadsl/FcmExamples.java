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
import org.apache.pekko.japi.Pair;
// #imports
import org.apache.pekko.stream.connectors.google.firebase.fcm.FcmSettings;
import org.apache.pekko.stream.connectors.google.firebase.fcm.v1.models.*;
import org.apache.pekko.stream.connectors.google.firebase.fcm.v1.javadsl.GoogleFcm;

// #imports
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class FcmExamples {

  private static void example() {
    ActorSystem system = ActorSystem.create();

    // #simple-send
    FcmSettings fcmConfig = FcmSettings.create();
    FcmNotification notification =
        FcmNotification.basic("Test", "This is a test notification!", new Token("token"));
    Source.single(notification).runWith(GoogleFcm.fireAndForget(fcmConfig), system);
    // #simple-send

    // #asFlow-send
    CompletionStage<List<FcmResponse>> result1 =
        Source.single(notification)
            .via(GoogleFcm.send(fcmConfig))
            .map(
                res -> {
                  if (res.isSuccess()) {
                    FcmSuccessResponse response = (FcmSuccessResponse) res;
                    System.out.println("Successful " + response.getName());
                  } else {
                    FcmErrorResponse response = (FcmErrorResponse) res;
                    System.out.println("Send error " + response.getRawError());
                  }
                  return res;
                })
            .runWith(Sink.seq(), system);
    // #asFlow-send

    // #withData-send
    CompletionStage<List<Pair<FcmResponse, String>>> result2 =
        Source.single(Pair.create(notification, "superData"))
            .via(GoogleFcm.sendWithPassThrough(fcmConfig))
            .runWith(Sink.seq(), system);
    // #withData-send

  }
}
