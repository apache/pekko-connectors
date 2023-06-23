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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.javadsl;

import org.apache.pekko.stream.Attributes;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryRecord;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryStorageSettings;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.BigQueryStorageSpecBase;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl.BigQueryStorageAttributes;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.storage.scaladsl.GrpcBigQueryStorageReader;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Sink;

import com.google.cloud.bigquery.storage.v1.DataFormat;
import com.google.cloud.bigquery.storage.v1.ReadSession;

import org.junit.*;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class BigQueryStorageSpec extends BigQueryStorageSpecBase {

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  @Test
  public void filterResultsBasedOnRowRestrictionConfigured()
      throws InterruptedException, ExecutionException, TimeoutException {
    AvroByteStringDecoder um = new AvroByteStringDecoder(FullAvroSchema());

    CompletionStage<List<List<BigQueryRecord>>> bigQueryRecords =
        BigQueryStorage.createMergedStreams(
                Project(),
                Dataset(),
                Table(),
                DataFormat.AVRO,
                ReadSession.TableReadOptions.newBuilder().setRowRestriction("true = false").build(),
                um)
            .withAttributes(mockBQReader())
            .runWith(Sink.seq(), system());

    assertTrue(
        "number of generic records should be more than 0",
        bigQueryRecords.toCompletableFuture().get(5, TimeUnit.SECONDS).isEmpty());
  }

  public Attributes mockBQReader() {
    return mockBQReader(bqHost(), bqPort());
  }

  public Attributes mockBQReader(String host, int port) {
    GrpcBigQueryStorageReader reader =
        GrpcBigQueryStorageReader.apply(BigQueryStorageSettings.create(host, port), system());
    return BigQueryStorageAttributes.reader(reader);
  }

  @Before
  public void initialize() {
    startMock();
  }

  @After
  public void tearDown() {
    stopMock();
    system().terminate();
  }
}
