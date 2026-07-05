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

package org.apache.pekko.stream.connectors.spring.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties for Pekko Streams Spring Web integration.
 *
 * <p>Properties are bound under the {@code pekko.stream.connectors.spring.web} prefix.
 */
@ConfigurationProperties(prefix = "pekko.stream.connectors.spring.web")
public class SpringWebPekkoStreamsProperties {

  private String actorSystemName;

  /**
   * Returns the name of the actor system to create. If not set, a default name is used.
   *
   * @return the actor system name, or {@code null} if not configured
   */
  public String getActorSystemName() {
    return actorSystemName;
  }

  /**
   * Sets the name of the actor system to create.
   *
   * @param actorSystemName the actor system name
   */
  public void setActorSystemName(String actorSystemName) {
    this.actorSystemName = actorSystemName;
  }
}
