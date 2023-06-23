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
// #prepared
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Function2;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.cassandra.CassandraWriteSettings;
import org.apache.pekko.stream.connectors.cassandra.javadsl.CassandraFlow;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.SourceWithContext;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
// #prepared
import org.apache.pekko.stream.connectors.cassandra.javadsl.CassandraSession;
import org.apache.pekko.stream.connectors.cassandra.javadsl.CassandraSource;
import org.apache.pekko.stream.connectors.cassandra.scaladsl.CassandraAccess;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import static docs.javadsl.CassandraTestHelper.await;

public class CassandraFlowTest {
  static final String TEST_NAME = "CassandraFlowTest";

  static CassandraTestHelper helper;

  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  @BeforeClass
  public static void beforeAll() {
    helper = new CassandraTestHelper(TEST_NAME);
  }

  @AfterClass
  public static void afterAll() {
    helper.shutdown();
  }

  ActorSystem system = helper.system;
  CassandraSession cassandraSession = helper.cassandraSession;
  CassandraAccess cassandraAccess = helper.cassandraAccess;

  @Test
  public void simpleUpdate() throws InterruptedException, ExecutionException, TimeoutException {
    String table = helper.createTableName();
    await(
        cassandraAccess.withSchemaMetadataDisabled(
            () ->
                cassandraAccess
                    .lifecycleSession()
                    .executeDDL("CREATE TABLE IF NOT EXISTS " + table + " (id int PRIMARY KEY);")));
    List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);

    CompletionStage<Done> written =
        Source.from(data)
            .via(
                CassandraFlow.create(
                    cassandraSession,
                    CassandraWriteSettings.defaults(),
                    "INSERT INTO " + table + "(id) VALUES (?)",
                    (element, preparedStatement) -> preparedStatement.bind(element)))
            .runWith(Sink.ignore(), system);

    assertThat(await(written), is(Done.done()));

    CompletionStage<List<Integer>> select =
        CassandraSource.create(cassandraSession, "SELECT * FROM " + table)
            .map(row -> row.getInt("id"))
            .runWith(Sink.seq(), system);
    List<Integer> rows = await(select);
    assertThat(new ArrayList<>(rows), hasItems(data.toArray()));
  }

  @Test
  public void typedUpdate() throws InterruptedException, ExecutionException, TimeoutException {
    String table = helper.createTableName();
    await(
        cassandraAccess.withSchemaMetadataDisabled(
            () ->
                cassandraAccess
                    .lifecycleSession()
                    .executeDDL(
                        "CREATE TABLE IF NOT EXISTS "
                            + table
                            + " (id int PRIMARY KEY, name text, city text);")));

    // #prepared

    List<Person> persons =
        Arrays.asList(
            new Person(12, "John", "London"),
            new Person(43, "Umberto", "Roma"),
            new Person(56, "James", "Chicago"));

    Function2<Person, PreparedStatement, BoundStatement> statementBinder =
        (person, preparedStatement) -> preparedStatement.bind(person.id, person.name, person.city);

    CompletionStage<List<Person>> written =
        Source.from(persons)
            .via(
                CassandraFlow.create(
                    cassandraSession,
                    CassandraWriteSettings.defaults(),
                    "INSERT INTO " + table + "(id, name, city) VALUES (?, ?, ?)",
                    statementBinder))
            .runWith(Sink.seq(), system);
    // #prepared

    assertThat(await(written).size(), is(persons.size()));

    CompletionStage<List<Person>> select =
        CassandraSource.create(cassandraSession, "SELECT * FROM " + table)
            .map(row -> new Person(row.getInt("id"), row.getString("name"), row.getString("city")))
            .runWith(Sink.seq(), system);
    List<Person> rows = await(select);
    assertThat(new ArrayList<>(rows), hasItems(persons.toArray()));
  }

  @Test
  public void withContextUsage() throws InterruptedException, ExecutionException, TimeoutException {
    String table = helper.createTableName();
    await(
        cassandraAccess.withSchemaMetadataDisabled(
            () ->
                cassandraAccess
                    .lifecycleSession()
                    .executeDDL(
                        "CREATE TABLE IF NOT EXISTS "
                            + table
                            + " (id int PRIMARY KEY, name text, city text);")));

    List<Pair<Person, AckHandle>> persons =
        Arrays.asList(
            Pair.create(new Person(12, "John", "London"), new AckHandle()),
            Pair.create(new Person(43, "Umberto", "Roma"), new AckHandle()),
            Pair.create(new Person(56, "James", "Chicago"), new AckHandle()));

    // #withContext
    SourceWithContext<Person, AckHandle, NotUsed> from = // ???;
        // #withContext
        SourceWithContext.fromPairs(Source.from(persons));
    // #withContext
    CompletionStage<Done> written =
        from.via(
                CassandraFlow.withContext(
                    cassandraSession,
                    CassandraWriteSettings.defaults(),
                    "INSERT INTO " + table + "(id, name, city) VALUES (?, ?, ?)",
                    (person, preparedStatement) ->
                        preparedStatement.bind(person.id, person.name, person.city)))
            .asSource()
            .mapAsync(1, pair -> pair.second().ack())
            .runWith(Sink.ignore(), system);
    // #withContext

    assertThat(await(written), is(Done.done()));

    CompletionStage<List<Person>> select =
        CassandraSource.create(cassandraSession, "SELECT * FROM " + table)
            .map(row -> new Person(row.getInt("id"), row.getString("name"), row.getString("city")))
            .runWith(Sink.seq(), system);
    List<Person> rows = await(select);
    assertThat(new ArrayList<>(rows), hasItems(persons.stream().map(p -> p.first()).toArray()));
  }

  public static final class Person {
    public final int id;
    public final String name;
    public final String city;

    public Person(int id, String name, String city) {
      this.id = id;
      this.name = name;
      this.city = city;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Person that = (Person) o;
      return Objects.equals(id, that.id)
          && Objects.equals(name, that.name)
          && Objects.equals(city, that.city);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, name, city);
    }
  }

  public static final class AckHandle {
    public CompletionStage<Done> ack() {
      return CompletableFuture.completedFuture(Done.done());
    }
  }
}
