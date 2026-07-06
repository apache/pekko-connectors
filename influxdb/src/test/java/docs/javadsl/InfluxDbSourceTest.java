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

import static docs.javadsl.TestUtils.cleanDatabase;
import static docs.javadsl.TestUtils.dropDatabase;
import static docs.javadsl.TestUtils.populateDatabase;
import static docs.javadsl.TestUtils.setupConnection;

import java.util.List;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.connectors.influxdb.InfluxDbReadSettings;
import org.apache.pekko.stream.connectors.influxdb.javadsl.InfluxDbSource;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingExtension;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.testkit.javadsl.StreamTestKit;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(LogCapturingExtension.class)
public class InfluxDbSourceTest {

  private static ActorSystem system;
  private static InfluxDB influxDB;

  private static final String DATABASE_NAME = "InfluxDbSourceTest";

  @BeforeAll
  public static void setupDatabase() {
    system = ActorSystem.create();

    influxDB = setupConnection(DATABASE_NAME);
  }

  @AfterAll
  public static void teardown() {
    dropDatabase(influxDB, DATABASE_NAME);
    TestKit.shutdownActorSystem(system);
  }

  @BeforeEach
  public void setUp() throws Throwable {
    populateDatabase(influxDB, InfluxDbSourceCpu.class);
  }

  @AfterEach
  public void cleanUp() {
    cleanDatabase(influxDB, DATABASE_NAME);
    StreamTestKit.assertAllStagesStopped(Materializer.matFromSystem(system));
  }

  @Test
  public void streamQueryResult() throws Exception {
    Query query = new Query("SELECT * FROM cpu", DATABASE_NAME);

    CompletionStage<List<InfluxDbSourceCpu>> rows =
        InfluxDbSource.typed(
                InfluxDbSourceCpu.class, InfluxDbReadSettings.Default(), influxDB, query)
            .runWith(Sink.seq(), system);

    List<InfluxDbSourceCpu> cpus = rows.toCompletableFuture().get();

    Assertions.assertEquals(2, cpus.size());
  }

  @Test
  public void streamRawQueryResult() throws Exception {
    Query query = new Query("SELECT * FROM cpu", DATABASE_NAME);

    CompletionStage<List<QueryResult>> completionStage =
        InfluxDbSource.create(influxDB, query).runWith(Sink.seq(), system);

    List<QueryResult> queryResults = completionStage.toCompletableFuture().get();
    QueryResult queryResult = queryResults.get(0);

    Assertions.assertFalse(queryResult.hasError());

    final int resultSize = queryResult.getResults().get(0).getSeries().get(0).getValues().size();

    Assertions.assertEquals(2, resultSize);
  }
}
