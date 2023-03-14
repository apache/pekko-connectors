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

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.AsPublisher;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.Assert;

import static org.springframework.core.ReactiveTypeDescriptor.multiValue;

public class PekkoStreamsRegistrar {

  private final ActorSystem system;

  /**
   * deprecated, use {@link #PekkoStreamsRegistrar(ClassicActorSystemProvider)}.
   *
   * @deprecated pass in the actor system instead of the materializer, since 3.0.0
   */
  @Deprecated
  public PekkoStreamsRegistrar(Materializer materializer) {
    this(materializer.system());
  }

  public PekkoStreamsRegistrar(ClassicActorSystemProvider system) {
    this.system = system.classicSystem();
  }

  public void registerAdapters(ReactiveAdapterRegistry registry) {
    Assert.notNull(registry, "registry must not be null");
    registry.registerReactiveType(
        multiValue(org.apache.pekko.stream.javadsl.Source.class, org.apache.pekko.stream.javadsl.Source::empty),
        source ->
            ((org.apache.pekko.stream.javadsl.Source<?, ?>) source)
                .runWith(org.apache.pekko.stream.javadsl.Sink.asPublisher(AsPublisher.WITH_FANOUT), system),
        org.apache.pekko.stream.javadsl.Source::fromPublisher);

    registry.registerReactiveType(
        multiValue(org.apache.pekko.stream.scaladsl.Source.class, org.apache.pekko.stream.scaladsl.Source::empty),
        source ->
            ((org.apache.pekko.stream.scaladsl.Source<?, ?>) source)
                .runWith(
                    org.apache.pekko.stream.scaladsl.Sink.asPublisher(true),
                    Materializer.matFromSystem(system)),
        org.apache.pekko.stream.scaladsl.Source::fromPublisher);
  }
}
