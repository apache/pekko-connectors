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
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class HBaseStageTest {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private static ActorSystem system;

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create();
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  // #create-converter-put
  Function<Person, List<Mutation>> hBaseConverter =
      person -> {
        try {
          Put put = new Put(String.format("id_%d", person.id).getBytes("UTF-8"));
          put.addColumn(
              "info".getBytes("UTF-8"), "name".getBytes("UTF-8"), person.name.getBytes("UTF-8"));

          return Collections.singletonList(put);
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
          Append append = new Append(String.format("id_%d", person.id).getBytes("UTF-8"));
          append.add(
              "info".getBytes("UTF-8"), "aliases".getBytes("UTF-8"), person.name.getBytes("UTF-8"));

          return Collections.singletonList(append);
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
          Delete delete = new Delete(String.format("id_%d", person.id).getBytes("UTF-8"));

          return Collections.singletonList(delete);
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
          Increment increment = new Increment(String.format("id_%d", person.id).getBytes("UTF-8"));
          increment.addColumn("info".getBytes("UTF-8"), "numberOfChanges".getBytes("UTF-8"), 1);

          return Collections.singletonList(increment);
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
          byte[] id = String.format("id_%d", person.id).getBytes("UTF-8");
          byte[] infoFamily = "info".getBytes("UTF-8");

          if (person.id != 0 && person.name.isEmpty()) {
            Delete delete = new Delete(id);
            return Collections.singletonList(delete);
          } else if (person.id != 0) {
            Put put = new Put(id);
            put.addColumn(infoFamily, "name".getBytes("UTF-8"), person.name.getBytes("UTF-8"));

            Increment increment = new Increment(id);
            increment.addColumn(infoFamily, "numberOfChanges".getBytes("UTF-8"), 1);

            return Arrays.asList(put, increment);
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
            Collections.singletonList("info"),
            hBaseConverter);
    // #create-settings

    // #sink
    final Sink<Person, CompletionStage<Done>> sink = HTableStage.sink(tableSettings);
    CompletionStage<Done> o =
        Source.from(Arrays.asList(100, 101, 102, 103, 104))
            .map((i) -> new Person(i, String.format("name %d", i)))
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
            Collections.singletonList("info"),
            hBaseConverter);

    // #flow
    Flow<Person, Person, NotUsed> flow = HTableStage.flow(tableSettings);
    Pair<NotUsed, CompletionStage<List<Person>>> run =
        Source.from(Arrays.asList(200, 201, 202, 203, 204))
            .map((i) -> new Person(i, String.format("name_%d", i)))
            .via(flow)
            .toMat(Sink.seq(), Keep.both())
            .run(system);
    // #flow

    assertEquals(5, run.second().toCompletableFuture().get().size());
  }

  @Test
  public void readFromSource()
      throws InterruptedException, TimeoutException, ExecutionException,
          UnsupportedEncodingException {

    HTableSettings<Person> tableSettings =
        HTableSettings.create(
            HBaseConfiguration.create(),
            TableName.valueOf("person1"),
            Collections.singletonList("info"),
            hBaseConverter);

    final Sink<Person, CompletionStage<Done>> sink = HTableStage.sink(tableSettings);
    CompletionStage<Done> o =
        Source.from(Arrays.asList(new Person(300, "name 300"))).runWith(sink, system);
    assertEquals(Done.getInstance(), o.toCompletableFuture().get(5, TimeUnit.SECONDS));

    // #source
    Scan scan = new Scan(new Get("id_300".getBytes("UTF-8")));

    CompletionStage<List<Result>> f =
        HTableStage.source(scan, tableSettings).runWith(Sink.seq(), system);
    // #source

    assertEquals(1, f.toCompletableFuture().get().size());
  }

  @Test
  public void appendThroughFlow() throws Exception {
    HTableSettings<Person> appendSettings = mutationTableSettings(appendHBaseConverter);

    // unique row per run: the sbt cross-build reruns this suite against the same HBase instance
    int id = randomRowId();
    CompletionStage<Done> f =
        Source.from(Arrays.asList(new Person(id, "-a"), new Person(id, "-b")))
            .via(HTableStage.flow(appendSettings))
            .runWith(Sink.ignore(), system);
    assertEquals(Done.getInstance(), f.toCompletableFuture().get(5, TimeUnit.SECONDS));

    List<Result> results = readRow(appendSettings, id);
    assertEquals(1, results.size());
    assertEquals(
        "-a-b",
        new String(
            results.get(0).getValue(bytes("info"), bytes("aliases")), StandardCharsets.UTF_8));
  }

  @Test
  public void incrementThroughFlow() throws Exception {
    HTableSettings<Person> incrementSettings = mutationTableSettings(incrementHBaseConverter);

    int id = randomRowId();
    CompletionStage<Done> f =
        Source.from(
                Arrays.asList(new Person(id, "inc"), new Person(id, "inc"), new Person(id, "inc")))
            .via(HTableStage.flow(incrementSettings))
            .runWith(Sink.ignore(), system);
    assertEquals(Done.getInstance(), f.toCompletableFuture().get(5, TimeUnit.SECONDS));

    List<Result> results = readRow(incrementSettings, id);
    assertEquals(1, results.size());
    assertEquals(
        3L, Bytes.toLong(results.get(0).getValue(bytes("info"), bytes("numberOfChanges"))));
  }

  @Test
  public void deleteThroughFlow() throws Exception {
    HTableSettings<Person> putSettings = mutationTableSettings(hBaseConverter);
    int id = randomRowId();
    CompletionStage<Done> put =
        Source.single(new Person(id, "to be deleted"))
            .runWith(HTableStage.sink(putSettings), system);
    assertEquals(Done.getInstance(), put.toCompletableFuture().get(5, TimeUnit.SECONDS));
    assertEquals(1, readRow(putSettings, id).size());

    HTableSettings<Person> deleteSettings = mutationTableSettings(deleteHBaseConverter);
    CompletionStage<Done> delete =
        Source.single(new Person(id, "to be deleted"))
            .via(HTableStage.flow(deleteSettings))
            .runWith(Sink.ignore(), system);
    assertEquals(Done.getInstance(), delete.toCompletableFuture().get(5, TimeUnit.SECONDS));

    assertEquals(0, readRow(putSettings, id).size());
  }

  @Test
  public void complexConverterThroughFlow() throws Exception {
    HTableSettings<Person> complexSettings = mutationTableSettings(complexHBaseConverter);

    int mixedId = randomRowId();
    int deletedId = randomRowId();
    CompletionStage<List<Person>> f =
        Source.from(
                Arrays.asList(
                    new Person(0, "skipped"),
                    new Person(mixedId, "mixed"),
                    new Person(deletedId, "")))
            .via(HTableStage.flow(complexSettings))
            .runWith(Sink.seq(), system);
    assertEquals(3, f.toCompletableFuture().get(5, TimeUnit.SECONDS).size());

    List<Result> results = readRow(complexSettings, mixedId);
    assertEquals(1, results.size());
    assertEquals(
        "mixed",
        new String(results.get(0).getValue(bytes("info"), bytes("name")), StandardCharsets.UTF_8));
    assertEquals(
        1L, Bytes.toLong(results.get(0).getValue(bytes("info"), bytes("numberOfChanges"))));
    assertEquals(0, readRow(complexSettings, 0).size());
    assertEquals(0, readRow(complexSettings, deletedId).size());
  }

  private static int randomRowId() {
    return 1000
        + java.util.concurrent.ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE - 1000);
  }

  private static byte[] bytes(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  private HTableSettings<Person> mutationTableSettings(Function<Person, List<Mutation>> converter) {
    return HTableSettings.create(
        HBaseConfiguration.create(),
        TableName.valueOf("person3"),
        Collections.singletonList("info"),
        converter);
  }

  private List<Result> readRow(HTableSettings<Person> tableSettings, int id) throws Exception {
    Scan scan = new Scan(new Get(String.format("id_%d", id).getBytes(StandardCharsets.UTF_8)));
    return HTableStage.source(scan, tableSettings)
        .runWith(Sink.seq(), system)
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
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
