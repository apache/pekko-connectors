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

package akka.stream.alpakka.pravega;

import akka.actor.ActorSystem;
import docs.javadsl.PravegaBaseTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PravegaAkkaTestCaseSupport {

  protected static final Logger LOGGER = LoggerFactory.getLogger(PravegaBaseTestCase.class);
  protected static ActorSystem system;

  public static void init() {
    system = ActorSystem.create();
  }
}
