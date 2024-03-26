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
import org.apache.pekko.stream.connectors.geode.javadsl.Geode;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class GeodeFiniteSourceTestCase extends GeodeBaseTestCase {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  @Test
  public void finiteSourceTest() throws ExecutionException, InterruptedException {

    Geode geode = createGeodeClient();

    // #query
    CompletionStage<Done> personsDone =
        geode
            .query("select * from /persons", new PersonPdxSerializer())
            .runForeach(
                p -> LOGGER.debug(p.toString()),
                system);
    // #query

    personsDone.toCompletableFuture().get();

    CompletionStage<Done> animalsDone =
        geode
            .query("select * from /animals", new AnimalPdxSerializer())
            .runForeach(
                p -> LOGGER.debug(p.toString()),
                system);

    animalsDone.toCompletableFuture().get();
    geode.close();
  }
}
