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

package org.apache.pekko.stream.connectors.slick.javadsl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Exercises the Java API additions of the Slick connector against an in-memory H2 database. */
public class SlickJavaApiTest {

  private static final String TABLE = "PEKKO_CONNECTORS_SLICK_JAVA_API_TEST";

  private static ActorSystem system;
  private static SlickSession session;

  @BeforeAll
  public static void setup() throws Exception {
    system = ActorSystem.create();
    SlickSession configured = SlickSession.forConfig("slick-h2");
    session = SlickSession.forDbAndProfile(configured.db(), configured.profile());
    execute("CREATE TABLE " + TABLE + "(ID INTEGER, NAME VARCHAR(50), DATA VARBINARY(8))");
    for (int i = 0; i < 5; i++) {
      execute("INSERT INTO " + TABLE + " VALUES (" + i + ", 'Name" + i + "', X'0102')");
    }
  }

  @AfterAll
  public static void teardown() throws Exception {
    execute("DROP TABLE " + TABLE);
    system.registerOnTermination(session::close);
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void parameterisedSource() throws Exception {
    List<String> names =
        Slick.source(
                session,
                "SELECT NAME FROM " + TABLE + " WHERE ID >= ? AND ID < ? ORDER BY ID",
                statement -> {
                  try {
                    statement.setInt(1, 1);
                    statement.setInt(2, 3);
                  } catch (SQLException e) {
                    throw new RuntimeException(e);
                  }
                },
                SlickRow::nextString)
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    assertEquals(Arrays.asList("Name1", "Name2"), names);
  }

  @Test
  public void primitiveByteArray() throws Exception {
    List<byte[]> data =
        Slick.source(
                session, "SELECT DATA FROM " + TABLE + " WHERE ID = 0", SlickRow::nextByteArray)
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    assertArrayEquals(new byte[] {1, 2}, data.get(0));
  }

  private static void execute(String statement) throws Exception {
    Source.single(statement)
        .runWith(Slick.sink(session, s -> s), system)
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
  }
}
