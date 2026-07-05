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

import static org.springframework.core.ReactiveTypeDescriptor.multiValue;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.AsPublisher;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.Assert;

/**
 * Registers Pekko Streams reactive type adapters (both Java and Scala DSL {@code Source} types)
 * with Spring's {@link ReactiveAdapterRegistry}, enabling Spring Web to handle Pekko Streams
 * sources as reactive return types in controller methods.
 */
public class PekkoStreamsRegistrar {

  private final ActorSystem system;

  /**
   * Creates a new registrar backed by the given actor system.
   *
   * @param system the actor system provider used to materialize Pekko Streams sources
   */
  public PekkoStreamsRegistrar(ClassicActorSystemProvider system) {
    this.system = system.classicSystem();
  }

  /**
   * Registers the Java and Scala DSL {@code Source} reactive types with the given registry.
   *
   * @param registry the Spring {@link ReactiveAdapterRegistry} to register adapters with
   */
  public void registerAdapters(ReactiveAdapterRegistry registry) {
    Assert.notNull(registry, "registry must not be null");
    registry.registerReactiveType(
        multiValue(
            org.apache.pekko.stream.javadsl.Source.class,
            org.apache.pekko.stream.javadsl.Source::empty),
        source ->
            ((org.apache.pekko.stream.javadsl.Source<?, ?>) source)
                .runWith(
                    org.apache.pekko.stream.javadsl.Sink.asPublisher(AsPublisher.WITH_FANOUT),
                    system),
        org.apache.pekko.stream.javadsl.Source::fromPublisher);

    registry.registerReactiveType(
        multiValue(
            org.apache.pekko.stream.scaladsl.Source.class,
            org.apache.pekko.stream.scaladsl.Source::empty),
        source ->
            ((org.apache.pekko.stream.scaladsl.Source<?, ?>) source)
                .runWith(
                    org.apache.pekko.stream.scaladsl.Sink.asPublisher(true),
                    Materializer.matFromSystem(system)),
        org.apache.pekko.stream.scaladsl.Source::fromPublisher);
  }
}
