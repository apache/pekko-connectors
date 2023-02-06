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
import org.apache.pekko.NotUsed;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.geode.javadsl.GeodeWithPoolSubscription;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class GeodeContinuousSourceTestCase extends GeodeBaseTestCase {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  @Test
  public void continuousSourceTest() throws ExecutionException, InterruptedException {

    GeodeWithPoolSubscription geode = createGeodeWithPoolSubscription();

    // #continuousQuery
    CompletionStage<Done> fut =
        geode
            .continuousQuery("test", "select * from /persons", new PersonPdxSerializer())
            .runForeach(
                p -> {
                  LOGGER.debug(p.toString());
                  if (p.getId() == 120) {
                    geode.closeContinuousQuery("test");
                  }
                },
                system);
    // #continuousQuery

    Flow<Person, Person, NotUsed> flow =
        geode.flow(personRegionSettings, new PersonPdxSerializer());

    Pair<NotUsed, CompletionStage<List<Person>>> run =
        Source.from(Arrays.asList(120))
            .map((i) -> new Person(i, String.format("Java flow %d", i), new Date()))
            .via(flow)
            .toMat(Sink.seq(), Keep.both())
            .run(system);

    run.second().toCompletableFuture().get();

    fut.toCompletableFuture().get();

    geode.close();
  }
}
