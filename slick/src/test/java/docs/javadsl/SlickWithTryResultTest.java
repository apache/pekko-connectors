/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package docs.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.function.Function2;
import org.apache.pekko.stream.connectors.slick.javadsl.Slick;
import org.apache.pekko.stream.connectors.slick.javadsl.SlickSession;
import org.apache.pekko.stream.connectors.slick.javadsl.SlickWithTryResult;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scala.util.Try;

public class SlickWithTryResultTest {

  private static final String TABLE = "PEKKO_CONNECTORS_SLICK_JAVADSL_TRY_TEST_USERS";

  private static ActorSystem system;
  private static SlickSession session;

  private static final Function<User, String> insertUser =
      user -> "INSERT INTO " + TABLE + " VALUES (" + user.id() + ", '" + user.name() + "')";

  private static final Function2<User, Connection, PreparedStatement> insertUserPS =
      (user, connection) -> {
        PreparedStatement statement =
            connection.prepareStatement("INSERT INTO " + TABLE + " VALUES (?, ?)");
        statement.setInt(1, user.id());
        statement.setString(2, user.name());
        return statement;
      };

  private static final User user1 = new User(1, "Name1");
  private static final User user2 = new User(2, "Name2");

  @BeforeAll
  public static void setup() throws Exception {
    system = ActorSystem.create();
    session = SlickSession.forConfig("slick-h2");
    execute("CREATE TABLE " + TABLE + "(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
  }

  @AfterEach
  public void cleanUp() throws Exception {
    execute("DELETE FROM " + TABLE);
  }

  @AfterAll
  public static void teardown() throws Exception {
    execute("DROP TABLE " + TABLE);
    system.registerOnTermination(session::close);
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void flowTryEmitsSuccessWithTheCount() throws Exception {
    List<Try<Integer>> results =
        await(
            Source.from(Arrays.asList(user1, user2))
                .via(SlickWithTryResult.flowTry(session, insertUser))
                .runWith(Sink.seq(), system));
    assertEquals(2, results.size());
    assertTrue(results.stream().allMatch(Try::isSuccess));
    assertEquals(Integer.valueOf(1), results.get(0).get());
  }

  @Test
  public void flowTryWithPreparedStatement() throws Exception {
    List<Try<Integer>> results =
        await(
            Source.from(Arrays.asList(user1, user2))
                .via(SlickWithTryResult.flowTry(session, insertUserPS))
                .runWith(Sink.seq(), system));
    assertTrue(results.stream().allMatch(Try::isSuccess));
  }

  @Test
  public void flowTryCarriesOnAfterAFailure() throws Exception {
    // #flowTry
    // the duplicate of user1 is a Failure, and the stream carries on with the next element
    CompletionStage<List<Try<Integer>>> result =
        Source.from(Arrays.asList(user1, user1, user2))
            .via(SlickWithTryResult.flowTry(session, insertUser))
            .runWith(Sink.seq(), system);
    // #flowTry
    List<Try<Integer>> results = await(result);
    assertEquals(
        Arrays.asList(true, false, true),
        results.stream().map(Try::isSuccess).collect(Collectors.toList()));
    assertEquals(2, countUsers());
  }

  @Test
  public void flowTryWithParallelism() throws Exception {
    List<Try<Integer>> results =
        await(
            Source.from(Arrays.asList(user1, user2))
                .via(SlickWithTryResult.flowTry(session, 4, insertUser))
                .runWith(Sink.seq(), system));
    assertTrue(results.stream().allMatch(Try::isSuccess));
  }

  @Test
  public void flowTryWithPassThroughMapsTheResult() throws Exception {
    List<Try<String>> results =
        await(
            Source.from(Arrays.asList(user1, user1))
                .via(
                    SlickWithTryResult.<User, String>flowTryWithPassThrough(
                        session,
                        system.dispatcher(),
                        insertUser,
                        (user, count) -> user.name() + ":" + count))
                .runWith(Sink.seq(), system));
    assertEquals("Name1:1", results.get(0).get());
    assertTrue(results.get(1).isFailure());
  }

  @Test
  public void flowTryWithPassThroughEmitsAFailureWhenTheMapperThrows() throws Exception {
    Function2<User, Integer, String> throwing =
        (user, count) -> {
          throw new IllegalStateException("mapper failed");
        };
    List<Try<String>> results =
        await(
            Source.single(user1)
                .via(
                    SlickWithTryResult.flowTryWithPassThrough(
                        session, system.dispatcher(), 1, insertUserPS, throwing))
                .runWith(Sink.seq(), system));
    assertTrue(results.get(0).failed().get() instanceof IllegalStateException);
  }

  @Test
  public void sinkTryMaterializesTheLastResult() throws Exception {
    Try<Integer> success =
        await(
            Source.from(Arrays.asList(user1, user2))
                .runWith(SlickWithTryResult.sinkTry(session, insertUser), system));
    assertEquals(Integer.valueOf(1), success.get());

    Try<Integer> failure =
        await(
            Source.single(user1)
                .runWith(SlickWithTryResult.sinkTry(session, 2, insertUserPS), system));
    assertFalse(failure.isSuccess());
  }

  private static int countUsers() throws Exception {
    return await(
            Slick.source(session, "SELECT ID FROM " + TABLE, row -> row.nextInt())
                .runWith(Sink.seq(), system))
        .size();
  }

  private static void execute(String statement) throws Exception {
    await(Source.single(statement).runWith(Slick.sink(session), system));
  }

  private static <T> T await(CompletionStage<T> stage) throws Exception {
    return stage.toCompletableFuture().get(10, TimeUnit.SECONDS);
  }
}
