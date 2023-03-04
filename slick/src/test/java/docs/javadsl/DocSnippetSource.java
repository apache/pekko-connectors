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

// #important-imports
import akka.stream.javadsl.*;
import akka.stream.alpakka.slick.javadsl.*;
// #important-imports

import java.util.concurrent.CompletionStage;

import akka.Done;
import akka.actor.ActorSystem;

public class DocSnippetSource {
  public static void main(String[] args) throws Exception {
    final ActorSystem system = ActorSystem.create();

    // #source-example
    final SlickSession session = SlickSession.forConfig("slick-h2");
    system.registerOnTermination(session::close);

    final CompletionStage<Done> done =
        Slick.source(
                session,
                "SELECT ID, NAME FROM ALPAKKA_SLICK_JAVADSL_TEST_USERS",
                (SlickRow row) -> new User(row.nextInt(), row.nextString()))
            .log("user")
            .runWith(Sink.ignore(), system);
    // #source-example

    done.whenComplete(
        (value, exception) -> {
          system.terminate();
        });
  }
}
