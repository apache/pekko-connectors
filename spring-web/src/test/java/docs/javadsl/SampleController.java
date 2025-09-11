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

// #use
import jakarta.annotation.PostConstruct;

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.event.LoggingAdapter;
import org.apache.pekko.stream.javadsl.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.Assert;

@RestController
public class SampleController {

  @Value("${org.apache.pekko.stream.connectors.spring.web.actor-system-name}")
  private String actorSystemName;

  @Autowired private ActorSystem system;

  @RequestMapping("/")
  public Source<String, NotUsed> index() {
    return Source.repeat("Hello world!").intersperse("\n").take(10);
  }

  @PostConstruct
  public void setup() {
    LoggingAdapter log = system.log();
    log.info("Injected ActorSystem Name -> {}", system.name());
    log.info("Property ActorSystemName -> {}", actorSystemName);
    Assert.isTrue((system.name().equals(actorSystemName)), "Validating ActorSystem name");
  }
}
// #use
