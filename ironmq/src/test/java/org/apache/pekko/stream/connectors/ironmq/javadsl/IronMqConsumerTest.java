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

package org.apache.pekko.stream.connectors.ironmq.javadsl;

import org.apache.pekko.stream.connectors.ironmq.IronMqSettings;
import org.apache.pekko.stream.connectors.ironmq.PushMessage;
import org.apache.pekko.stream.connectors.ironmq.UnitTest;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Keep;
import org.apache.pekko.stream.javadsl.Sink;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IronMqConsumerTest extends UnitTest {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  @Test
  public void ironMqConsumerShouldBeNiceToMe() throws Exception {

    String from = givenQueue();
    String to = givenQueue();
    givenMessages(from, 100);

    IronMqSettings settings = IronMqSettings.create(getActorSystem());

    int expectedNumberOfMessages = 10;

    int numberOfMessages =
        IronMqConsumer.atLeastOnceSource(from, settings)
            .take(expectedNumberOfMessages)
            .map(cm -> new CommittablePushMessage<>(PushMessage.create(cm.message().body()), cm))
            .alsoToMat(Sink.fold(0, (x, y) -> x + 1), Keep.right())
            .to(IronMqProducer.atLeastOnceSink(to, settings))
            .run(getMaterializer())
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

    assertThat(numberOfMessages, is(expectedNumberOfMessages));
  }
}
