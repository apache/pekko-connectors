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

import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapterRegistry;

// #configure

import org.apache.pekko.actor.ActorSystem;

@Configuration
@ConditionalOnClass(org.apache.pekko.stream.javadsl.Source.class)
@EnableConfigurationProperties(SpringWebPekkoStreamsProperties.class)
public class SpringWebPekkoStreamsConfiguration {

  private static final String DEFAULT_FACTORY_SYSTEM_NAME = "Pekko SpringWebPekkoStreamsSystem";

  private final ActorSystem system;
  private final SpringWebPekkoStreamsProperties properties;

  public SpringWebPekkoStreamsConfiguration(final SpringWebPekkoStreamsProperties properties) {
    this.properties = properties;
    final ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();

    system = ActorSystem.create(getActorSystemName(properties));
    new PekkoStreamsRegistrar(system).registerAdapters(registry);
  }

  @Bean
  @ConditionalOnMissingBean(ActorSystem.class)
  public ActorSystem getActorSystem() {
    return system;
  }

  public SpringWebPekkoStreamsProperties getProperties() {
    return properties;
  }

  private String getActorSystemName(final SpringWebPekkoStreamsProperties properties) {
    Objects.requireNonNull(
        properties,
        String.format(
            "%s is not present in application context",
            SpringWebPekkoStreamsProperties.class.getSimpleName()));

    if (isBlank(properties.getActorSystemName())) {
      return DEFAULT_FACTORY_SYSTEM_NAME;
    }

    return properties.getActorSystemName();
  }

  private boolean isBlank(String str) {
    return (str == null || str.isEmpty());
  }
}

// #configure
