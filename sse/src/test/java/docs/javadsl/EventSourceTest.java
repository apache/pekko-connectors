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

package docs.javadsl;

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;

import org.apache.pekko.stream.ThrottleMode;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;

import java.time.Duration;
import java.util.Optional;

// #event-source
import java.util.function.Function;
import java.util.concurrent.CompletionStage;

import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.model.*;
import org.apache.pekko.http.javadsl.model.sse.ServerSentEvent;
import org.apache.pekko.stream.connectors.sse.javadsl.EventSource;
// #event-source

public class EventSourceTest {

  @SuppressWarnings("ConstantConditions")
  public static void compileTest() {

    String host = "localhost";
    int port = 8080;
    ActorSystem system = null;

    int nrOfSamples = 10;

    // #event-source

    final Http http = Http.get(system);
    Function<HttpRequest, CompletionStage<HttpResponse>> send =
        (request) -> http.singleRequest(request);

    final Uri targetUri = Uri.create(String.format("http://%s:%d", host, port));
    final Optional<String> lastEventId = Optional.of("2");
    Source<ServerSentEvent, NotUsed> eventSource =
        EventSource.create(targetUri, send, lastEventId, system);
    // #event-source

    // #consume-events
    int elements = 1;
    Duration per = Duration.ofMillis(500);
    int maximumBurst = 1;

    eventSource
        .throttle(elements, per, maximumBurst, ThrottleMode.shaping())
        .take(nrOfSamples)
        .runWith(Sink.seq(), system);
    // #consume-events
  }
}
