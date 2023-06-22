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

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.geode.javadsl.Geode;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.RunnableGraph;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class GeodeSinkTestCase extends GeodeBaseTestCase {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  @Test
  public void sinkTest() throws ExecutionException, InterruptedException {

    Geode geode = createGeodeClient();

    Sink<Person, CompletionStage<Done>> sink =
        geode.sink(personRegionSettings, new PersonPdxSerializer());

    Source<Person, NotUsed> source = buildPersonsSource(100, 101, 103, 104, 105);

    RunnableGraph<CompletionStage<Done>> runnableGraph = source.toMat(sink, Keep.right());

    CompletionStage<Done> stage = runnableGraph.run(system);

    stage.toCompletableFuture().get();

    geode.close();
  }

  @Test
  public void sinkAnimalTest() throws ExecutionException, InterruptedException {

    Geode geode = createGeodeClient();

    Source<Animal, NotUsed> source = buildAnimalsSource(100, 101, 103, 104, 105);

    // #sink
    Sink<Animal, CompletionStage<Done>> sink =
        geode.sink(animalRegionSettings, new AnimalPdxSerializer());

    RunnableGraph<CompletionStage<Done>> runnableGraph = source.toMat(sink, Keep.right());
    // #sink

    CompletionStage<Done> stage = runnableGraph.run(system);

    stage.toCompletableFuture().get();

    geode.close();
  }
}
