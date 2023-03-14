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

package org.apache.pekko.stream.connectors.spring.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pekko.stream.connectors.spring.web")
public class SpringWebPekkoStreamsProperties {

  private String actorSystemName;

  public String getActorSystemName() {
    return actorSystemName;
  }

  public void setActorSystemName(String actorSystemName) {
    this.actorSystemName = actorSystemName;
  }
}
