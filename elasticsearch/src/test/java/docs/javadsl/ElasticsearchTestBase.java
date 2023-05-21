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

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.stream.connectors.elasticsearch.ApiVersion;
import org.apache.pekko.stream.connectors.elasticsearch.ApiVersionBase;
import org.apache.pekko.stream.connectors.elasticsearch.ElasticsearchConnectionSettings;
import org.apache.pekko.stream.connectors.elasticsearch.ElasticsearchParams;
import org.apache.pekko.stream.connectors.elasticsearch.OpensearchApiVersion;
import org.apache.pekko.stream.connectors.elasticsearch.OpensearchParams;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ElasticsearchTestBase {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  protected static ElasticsearchConnectionSettings connectionSettings;
  protected static ActorSystem system;
  protected static Http http;

  // #define-class
  public static class Book {
    public String title;

    public Book() {}

    public Book(String title) {
      this.title = title;
    }
  }
  // #define-class

  @BeforeClass
  public static void setupBase() {
    system = ActorSystem.create();
    http = Http.get(system);
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  protected static void prepareIndex(
      int port, org.apache.pekko.stream.connectors.elasticsearch.ApiVersionBase version) throws IOException {
    connectionSettings =
        ElasticsearchConnectionSettings.create(String.format("http://localhost:%d", port));

    register("source", "Pekko in Action");
    register("source", "Programming in Scala");
    register("source", "Learning Scala");
    register("source", "Scala for Spark in Production");
    register("source", "Scala Puzzlers");
    register("source", "Effective Pekko");
    register("source", "Pekko Concurrency");
    flushAndRefresh("source");
  }

  protected static void cleanIndex() throws IOException {
    HttpRequest request =
        HttpRequest.DELETE(String.format("%s/_all", connectionSettings.baseUrl()));
    http.singleRequest(request).toCompletableFuture().join();
  }

  protected static void flushAndRefresh(String indexName) throws IOException {
    HttpRequest flushRequest =
        HttpRequest.POST(String.format("%s/%s/_flush", connectionSettings.baseUrl(), indexName));
    http.singleRequest(flushRequest).toCompletableFuture().join();

    HttpRequest refreshRequest =
        HttpRequest.POST(String.format("%s/%s/_refresh", connectionSettings.baseUrl(), indexName));
    http.singleRequest(refreshRequest).toCompletableFuture().join();
  }

  protected static void register(String indexName, String title) {
    HttpRequest request =
        HttpRequest.POST(String.format("%s/%s/_doc", connectionSettings.baseUrl(), indexName))
            .withEntity(ContentTypes.APPLICATION_JSON, String.format("{\"title\": \"%s\"}", title));

    http.singleRequest(request).toCompletableFuture().join();
  }

  // #custom-search-params
  public static class TestDoc {
    public String id;
    public String a;
    public String b;
    public String c;

    // #custom-search-params
    public TestDoc() {}

    public TestDoc(String id, String a, String b, String c) {
      this.id = id;
      this.a = a;
      this.b = b;
      this.c = c;
    }
    // #custom-search-params
  }
  // #custom-search-params

  static class KafkaCommitter {
    List<Integer> committedOffsets = new ArrayList<>();

    public KafkaCommitter() {}

    void commit(KafkaOffset offset) {
      committedOffsets.add(offset.offset);
    }
  }

  static class KafkaOffset {
    final int offset;

    public KafkaOffset(int offset) {
      this.offset = offset;
    }
  }

  static class KafkaMessage {
    final Book book;
    final KafkaOffset offset;

    public KafkaMessage(Book book, KafkaOffset offset) {
      this.book = book;
      this.offset = offset;
    }
  }

  protected ElasticsearchParams constructElasticsearchParams(
      String indexName, String typeName, ApiVersionBase apiVersion) {
    if (apiVersion == ApiVersion.V5) {
      return ElasticsearchParams.V5(indexName, typeName);
    } else if (apiVersion == ApiVersion.V7) {
      return ElasticsearchParams.V7(indexName);
    } else if (apiVersion == OpensearchApiVersion.V1) {
      return OpensearchParams.V1(indexName);
    } else {
      throw new IllegalArgumentException("API version " + apiVersion + " is not supported");
    }
  }
}
