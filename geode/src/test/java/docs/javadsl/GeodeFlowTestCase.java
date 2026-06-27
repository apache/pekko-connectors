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
import org.apache.pekko.stream.connectors.geode.javadsl.Geode;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@ExtendWith(LogCapturingExtension.class)
public class GeodeFlowTestCase extends GeodeBaseTestCase {

  @Test
  public void flow() throws ExecutionException, InterruptedException {

    Geode geode = createGeodeClient();

    Source<Person, NotUsed> source = buildPersonsSource(110, 111, 113, 114, 115);

    // #flow
    Flow<Person, Person, NotUsed> flow =
        geode.flow(personRegionSettings, new PersonPdxSerializer());

    CompletionStage<List<Person>> run =
        source.via(flow).toMat(Sink.seq(), Keep.right()).run(system);
    // #flow

    run.toCompletableFuture().get();

    geode.close();
  }
}
