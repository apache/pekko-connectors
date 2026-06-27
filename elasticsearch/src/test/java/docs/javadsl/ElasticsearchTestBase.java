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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingExtension;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(LogCapturingExtension.class)
public class ElasticsearchTestBase {

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

  @BeforeAll
  public static void setupBase() {
    system = ActorSystem.create();
    http = Http.get(system);
  }

  @AfterAll
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  protected static void prepareIndex(
      int port, org.apache.pekko.stream.connectors.elasticsearch.ApiVersionBase version)
      throws IOException {
    connectionSettings =
        ElasticsearchConnectionSettings.create("http://localhost:%d".formatted(port));

    register("source", "Akka in Action");
    register("source", "Programming in Scala");
    register("source", "Learning Scala");
    register("source", "Scala for Spark in Production");
    register("source", "Scala Puzzlers");
    register("source", "Effective Akka");
    register("source", "Akka Concurrency");
    flushAndRefresh("source");
  }

  protected static void cleanIndex() throws IOException {
    HttpRequest request = HttpRequest.DELETE("%s/_all".formatted(connectionSettings.baseUrl()));
    http.singleRequest(request).toCompletableFuture().join();
  }

  protected static void flushAndRefresh(String indexName) throws IOException {
    HttpRequest flushRequest =
        HttpRequest.POST("%s/%s/_flush".formatted(connectionSettings.baseUrl(), indexName));
    http.singleRequest(flushRequest).toCompletableFuture().join();

    HttpRequest refreshRequest =
        HttpRequest.POST("%s/%s/_refresh".formatted(connectionSettings.baseUrl(), indexName));
    http.singleRequest(refreshRequest).toCompletableFuture().join();
  }

  protected static void register(String indexName, String title) {
    HttpRequest request =
        HttpRequest.POST("%s/%s/_doc".formatted(connectionSettings.baseUrl(), indexName))
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"title\": \"%s\"}".formatted(title));

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
