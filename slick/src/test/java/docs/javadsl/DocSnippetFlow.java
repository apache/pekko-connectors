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

import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.slick.javadsl.Slick;
import org.apache.pekko.stream.connectors.slick.javadsl.SlickSession;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DocSnippetFlow {
  public static void main(String[] args) throws Exception {
    final ActorSystem system = ActorSystem.create();

    // #flow-example
    final SlickSession session = SlickSession.forConfig("slick-h2");
    system.registerOnTermination(session::close);

    final List<User> users =
        IntStream.range(0, 42)
            .boxed()
            .map((i) -> new User(i, "Name" + i))
            .collect(Collectors.toList());

    int parallelism = 1;

    final CompletionStage<Done> done =
        Source.from(users)
            .via(
                Slick.flow(
                    session,
                    parallelism,
                    (user, connection) -> {
                      PreparedStatement statement =
                          connection.prepareStatement(
                              "INSERT INTO ALPAKKA_SLICK_JAVADSL_TEST_USERS VALUES (?, ?)");
                      statement.setInt(1, user.id);
                      statement.setString(2, user.name);
                      return statement;
                    }))
            .log("nr-of-updated-rows")
            .runWith(Sink.ignore(), system);
    // #flow-example

    done.whenComplete(
        (value, exception) -> {
          system.terminate();
        });
  }
}
