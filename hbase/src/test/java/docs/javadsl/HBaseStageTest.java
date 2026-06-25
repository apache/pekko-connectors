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
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.hbase.HTableSettings;
import org.apache.pekko.stream.connectors.hbase.javadsl.HTableStage;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(LogCapturingExtension.class)
public class HBaseStageTest {

  private static ActorSystem system;

  @BeforeAll
  public static void setup() {
    system = ActorSystem.create();
  }

  @AfterAll
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  // #create-converter-put
  Function<Person, List<Mutation>> hBaseConverter =
      person -> {
        try {
          Put put = new Put("id_%d".formatted(person.id).getBytes("UTF-8"));
          put.addColumn(
              "info".getBytes("UTF-8"), "name".getBytes("UTF-8"), person.name.getBytes("UTF-8"));

          return List.of(put);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          return Collections.emptyList();
        }
      };
  // #create-converter-put

  // #create-converter-append
  Function<Person, List<Mutation>> appendHBaseConverter =
      person -> {
        try {
          Append append = new Append("id_%d".formatted(person.id).getBytes("UTF-8"));
          append.add(
              "info".getBytes("UTF-8"), "aliases".getBytes("UTF-8"), person.name.getBytes("UTF-8"));

          return List.of(append);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          return Collections.emptyList();
        }
      };
  // #create-converter-append

  // #create-converter-delete
  Function<Person, List<Mutation>> deleteHBaseConverter =
      person -> {
        try {
          Delete delete = new Delete("id_%d".formatted(person.id).getBytes("UTF-8"));

          return List.of(delete);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          return Collections.emptyList();
        }
      };
  // #create-converter-delete

  // #create-converter-increment
  Function<Person, List<Mutation>> incrementHBaseConverter =
      person -> {
        try {
          Increment increment = new Increment("id_%d".formatted(person.id).getBytes("UTF-8"));
          increment.addColumn("info".getBytes("UTF-8"), "numberOfChanges".getBytes("UTF-8"), 1);

          return List.of(increment);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          return Collections.emptyList();
        }
      };
  // #create-converter-increment

  // #create-converter-complex
  Function<Person, List<Mutation>> complexHBaseConverter =
      person -> {
        try {
          byte[] id = "id_%d".formatted(person.id).getBytes("UTF-8");
          byte[] infoFamily = "info".getBytes("UTF-8");

          if (person.id != 0 && person.name.isEmpty()) {
            Delete delete = new Delete(id);
            return List.of(delete);
          } else if (person.id != 0) {
            Put put = new Put(id);
            put.addColumn(infoFamily, "name".getBytes("UTF-8"), person.name.getBytes("UTF-8"));

            Increment increment = new Increment(id);
            increment.addColumn(infoFamily, "numberOfChanges".getBytes("UTF-8"), 1);

            return List.of(put, increment);
          } else {
            return Collections.emptyList();
          }
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          return Collections.emptyList();
        }
      };

  // #create-converter-complex

  @Test
  public void writeToSink() throws InterruptedException, TimeoutException, ExecutionException {

    // #create-settings
    HTableSettings<Person> tableSettings =
        HTableSettings.create(
            HBaseConfiguration.create(),
            TableName.valueOf("person1"),
            List.of("info"),
            hBaseConverter);
    // #create-settings

    // #sink
    final Sink<Person, CompletionStage<Done>> sink = HTableStage.sink(tableSettings);
    CompletionStage<Done> o =
        Source.from(List.of(100, 101, 102, 103, 104))
            .map((i) -> new Person(i, "name %d".formatted(i)))
            .runWith(sink, system);
    // #sink

    assertEquals(Done.getInstance(), o.toCompletableFuture().get(5, TimeUnit.SECONDS));
  }

  @Test
  public void writeThroughFlow() throws ExecutionException, InterruptedException {

    HTableSettings<Person> tableSettings =
        HTableSettings.create(
            HBaseConfiguration.create(),
            TableName.valueOf("person2"),
            List.of("info"),
            hBaseConverter);

    // #flow
    Flow<Person, Person, NotUsed> flow = HTableStage.flow(tableSettings);
    Pair<NotUsed, CompletionStage<List<Person>>> run =
        Source.from(List.of(200, 201, 202, 203, 204))
            .map((i) -> new Person(i, "name_%d".formatted(i)))
            .via(flow)
            .toMat(Sink.seq(), Keep.both())
            .run(system);
    // #flow

    assertEquals(5, run.second().toCompletableFuture().get().size());
  }

  @Test
  public void readFromSource()
      throws InterruptedException,
          TimeoutException,
          ExecutionException,
          UnsupportedEncodingException {

    HTableSettings<Person> tableSettings =
        HTableSettings.create(
            HBaseConfiguration.create(),
            TableName.valueOf("person1"),
            List.of("info"),
            hBaseConverter);

    final Sink<Person, CompletionStage<Done>> sink = HTableStage.sink(tableSettings);
    CompletionStage<Done> o =
        Source.from(List.of(new Person(300, "name 300"))).runWith(sink, system);
    assertEquals(Done.getInstance(), o.toCompletableFuture().get(5, TimeUnit.SECONDS));

    // #source
    Scan scan = new Scan(new Get("id_300".getBytes("UTF-8")));

    CompletionStage<List<Result>> f =
        HTableStage.source(scan, tableSettings).runWith(Sink.seq(), system);
    // #source

    assertEquals(1, f.toCompletableFuture().get().size());
  }
}

class Person {

  int id;
  String name;

  public Person(int i, String name) {
    this.id = i;
    this.name = name;
  }
}
