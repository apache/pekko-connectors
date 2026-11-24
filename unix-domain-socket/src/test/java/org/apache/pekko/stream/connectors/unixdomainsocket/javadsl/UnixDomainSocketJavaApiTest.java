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

package org.apache.pekko.stream.connectors.unixdomainsocket.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.unixdomainsocket.UnixSocketAddress;
import org.apache.pekko.stream.connectors.unixdomainsocket.javadsl.UnixDomainSocket.ServerBinding;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Exercises the Java API additions of the Unix domain socket connector. */
public class UnixDomainSocketJavaApiTest {

  private static ActorSystem system;

  @BeforeAll
  public static void setup() {
    system = ActorSystem.create();
  }

  @AfterAll
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void bindAndHandleWithJavaDurationTimeout() throws Exception {
    Path path = Files.createTempDirectory("UnixDomainSocketJavaApiTest").resolve("sock");
    path.toFile().deleteOnExit();
    UnixDomainSocket uds = UnixDomainSocket.get(system);

    // halfClose = true: the client closes its write side right after sending, and the server must
    // still be able to write the reply rather than tearing the connection down on end of input
    ServerBinding binding =
        uds.bindAndHandle(
                Flow.of(ByteString.class)
                    .map(bytes -> ByteString.fromString(bytes.utf8String().toUpperCase())),
                path,
                128,
                true)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    List<ByteString> response =
        Source.single(ByteString.fromString("hello"))
            .via(
                uds.outgoingConnection(
                    new UnixSocketAddress(path), Optional.empty(), true, Duration.ofSeconds(5)))
            .runWith(Sink.seq(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    assertEquals(
        ByteString.fromString("HELLO"),
        response.stream().reduce(ByteString.emptyByteString(), ByteString::concat));

    CompletionStage<Done> unbound = binding.unbindDone();
    assertEquals(Done.getInstance(), unbound.toCompletableFuture().get(5, TimeUnit.SECONDS));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void deprecatedUnbindStillCompletesWithScalaUnit() throws Exception {
    Path path = Files.createTempDirectory("uds-java-api").resolve("unbind.sock");
    path.toFile().deleteOnExit();

    UnixDomainSocket uds = UnixDomainSocket.get(system);
    UnixDomainSocket.ServerBinding binding =
        uds.bindAndHandle(Flow.<ByteString>create(), path)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    // callers compiled against the previous signature retrieve the value as a BoxedUnit
    CompletionStage<scala.runtime.BoxedUnit> unbound = binding.unbind();
    assertEquals(
        scala.runtime.BoxedUnit.UNIT, unbound.toCompletableFuture().get(5, TimeUnit.SECONDS));
  }
}
