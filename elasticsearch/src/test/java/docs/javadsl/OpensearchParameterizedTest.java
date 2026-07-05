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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.elasticsearch.*;
import org.apache.pekko.stream.connectors.elasticsearch.javadsl.ElasticsearchFlow;
import org.apache.pekko.stream.connectors.elasticsearch.javadsl.ElasticsearchSource;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OpensearchParameterizedTest extends ElasticsearchTestBase {
  private OpensearchApiVersion apiVersion;

  public static Stream<Arguments> data() {
    return Stream.of(Arguments.of(9203, OpensearchApiVersion.V1));
  }

  @AfterEach
  public void afterParam() throws IOException {
    cleanIndex();
  }

  private void documentation() {
    // #connection-settings
    ElasticsearchConnectionSettings connectionSettings =
        OpensearchConnectionSettings.create("http://localhost:9200")
            .withCredentials("user", "password");
    // #connection-settings

    // #source-settings
    OpensearchSourceSettings sourceSettings =
        OpensearchSourceSettings.create(connectionSettings).withBufferSize(10);
    // #source-settings
    // #sink-settings
    OpensearchWriteSettings settings =
        OpensearchWriteSettings.create(connectionSettings)
            .withBufferSize(10)
            .withVersionType("internal")
            .withRetryLogic(RetryAtFixedRate.create(5, Duration.ofSeconds(1)))
            .withApiVersion(OpensearchApiVersion.V1);
    // #sink-settings

    // #opensearch-params
    ElasticsearchParams opensearchParams = OpensearchParams.V1("source");
    // #opensearch-params
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testUsingVersions(int port, OpensearchApiVersion apiVersion) throws Exception {
    this.apiVersion = apiVersion;
    prepareIndex(port, apiVersion);
    // Since the scala-test does a lot more logic testing,
    // all we need to test here is that we can receive and send version

    String indexName = "test_using_versions";
    String typeName = "_doc";

    // Insert document
    Book book = new Book("b");
    Source.single(WriteMessage.createIndexMessage("1", book))
        .via(
            ElasticsearchFlow.create(
                constructElasticsearchParams(indexName, typeName, apiVersion),
                OpensearchWriteSettings.create(connectionSettings)
                    .withApiVersion(apiVersion)
                    .withBufferSize(5),
                new ObjectMapper()))
        .runWith(Sink.seq(), system)
        .toCompletableFuture()
        .get();

    flushAndRefresh(indexName);

    // Search document and assert it having version 1
    ReadResult<Book> message =
        ElasticsearchSource.<Book>typed(
                constructElasticsearchParams(indexName, typeName, apiVersion),
                "{\"match_all\": {}}",
                OpensearchSourceSettings.create(connectionSettings)
                    .withApiVersion(apiVersion)
                    .withIncludeDocumentVersion(true),
                Book.class)
            .runWith(Sink.head(), system)
            .toCompletableFuture()
            .get();

    assertEquals(1L, message.version().get());

    flushAndRefresh(indexName);

    // Update document to version 2
    Source.single(WriteMessage.createIndexMessage("1", book).withVersion(1L))
        .via(
            ElasticsearchFlow.create(
                constructElasticsearchParams(indexName, typeName, apiVersion),
                OpensearchWriteSettings.create(connectionSettings)
                    .withApiVersion(apiVersion)
                    .withBufferSize(5)
                    .withVersionType("external"),
                new ObjectMapper()))
        .runWith(Sink.seq(), system)
        .toCompletableFuture()
        .get();

    flushAndRefresh(indexName);

    // Try to update document with wrong version to assert that we can send it
    long oldVersion = 1;
    boolean success =
        Source.single(WriteMessage.createIndexMessage("1", book).withVersion(oldVersion))
            .via(
                ElasticsearchFlow.create(
                    constructElasticsearchParams(indexName, typeName, apiVersion),
                    OpensearchWriteSettings.create(connectionSettings)
                        .withApiVersion(apiVersion)
                        .withBufferSize(5)
                        .withVersionType("external"),
                    new ObjectMapper()))
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get()
            .get(0)
            .success();

    assertEquals(false, success);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testUsingVersionType(int port, OpensearchApiVersion apiVersion) throws Exception {
    this.apiVersion = apiVersion;
    prepareIndex(port, apiVersion);
    String indexName = "book-test-version-type";
    String typeName = "_doc";

    Book book = new Book("A sample title");
    String docId = "1";
    long externalVersion = 5;

    // Insert new document using external version
    Source.single(WriteMessage.createIndexMessage("1", book).withVersion(externalVersion))
        .via(
            ElasticsearchFlow.create(
                constructElasticsearchParams(indexName, typeName, apiVersion),
                OpensearchWriteSettings.create(connectionSettings)
                    .withApiVersion(apiVersion)
                    .withBufferSize(5)
                    .withVersionType("external"),
                new ObjectMapper()))
        .runWith(Sink.seq(), system)
        .toCompletableFuture()
        .get();

    flushAndRefresh(indexName);

    // Assert that the document's external version is saved
    ReadResult<Book> message =
        ElasticsearchSource.<Book>typed(
                constructElasticsearchParams(indexName, typeName, apiVersion),
                "{\"match_all\": {}}",
                OpensearchSourceSettings.create(connectionSettings)
                    .withApiVersion(apiVersion)
                    .withIncludeDocumentVersion(true),
                Book.class)
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get()
            .get(0);

    assertEquals(externalVersion, message.version().get());
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testMultipleIndicesWithNoMatching(int port, OpensearchApiVersion apiVersion)
      throws Exception {
    this.apiVersion = apiVersion;
    prepareIndex(port, apiVersion);
    String indexName = "missing-*";
    String typeName = "_doc";

    // Assert that the document's external version is saved
    List<ReadResult<Book>> readResults =
        ElasticsearchSource.<Book>typed(
                constructElasticsearchParams(indexName, typeName, apiVersion),
                "{\"match_all\": {}}",
                OpensearchSourceSettings.create(connectionSettings).withApiVersion(apiVersion),
                Book.class)
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get();

    assertTrue(readResults.isEmpty());
  }

  public void compileOnlySample() {
    String doc = "dummy-doc";

    // #custom-index-name-example
    WriteMessage<String, NotUsed> msg =
        WriteMessage.createIndexMessage(doc).withIndexName("my-index");
    // #custom-index-name-example

    // #custom-metadata-example
    Map<String, String> metadata = new HashMap<>();
    metadata.put("pipeline", "myPipeline");
    WriteMessage<String, NotUsed> msgWithMetadata =
        WriteMessage.createIndexMessage(doc).withCustomMetadata(metadata);
    // #custom-metadata-example
  }
}
