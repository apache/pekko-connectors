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

package org.apache.pekko.stream.connectors.ironmq;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.connectors.ironmq.impl.IronMqClient;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.pekko.util.FutureConverters.*;
import static scala.collection.JavaConverters.*;

public abstract class UnitTest {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  private ActorSystem system;
  private Materializer materializer;
  private IronMqClient ironMqClient;

  @Before
  public void setup() throws Exception {
    Config config = initConfig();
    system = ActorSystem.create("TestActorSystem", config);
    materializer = Materializer.matFromSystem(system);
    ironMqClient =
        new IronMqClient(
            IronMqSettings.create(config.getConfig(IronMqSettings.ConfigPath())),
            system,
            materializer);
  }

  @After
  public void teardown() throws Exception {
    materializer.shutdown();
    TestKit.shutdownActorSystem(system);
  }

  protected Config initConfig() {
    String projectId = "project-" + System.currentTimeMillis();
    return ConfigFactory.parseString(
            "pekko.connectors.ironmq.credentials.project-id = " + projectId)
        .withFallback(ConfigFactory.load());
  }

  protected ActorSystem getActorSystem() {
    if (system == null) throw new IllegalStateException("The ActorSystem is not yet initialized");
    return system;
  }

  protected Materializer getMaterializer() {
    if (materializer == null)
      throw new IllegalStateException("The Materializer is not yet initialized");
    return materializer;
  }

  public IronMqClient getIronMqClient() {
    if (ironMqClient == null)
      throw new IllegalStateException("The IronMqClient is not yet initialized");
    return ironMqClient;
  }

  protected String givenQueue() {
    return givenQueue("test-" + UUID.randomUUID());
  }

  protected String givenQueue(String name) {
    try {
      return asJava(ironMqClient.createQueue(name, system.dispatcher()))
          .toCompletableFuture()
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Message.Ids givenMessages(String queueName, int n) {

    List<PushMessage> messages =
        IntStream.rangeClosed(1, n)
            .mapToObj(i -> PushMessage.create("test-" + i))
            .collect(Collectors.toList());

    try {
      return asJava(
              ironMqClient.pushMessages(
                  queueName,
                  asScalaBufferConverter(messages).asScala().toSeq(),
                  system.dispatcher()))
          .toCompletableFuture()
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
