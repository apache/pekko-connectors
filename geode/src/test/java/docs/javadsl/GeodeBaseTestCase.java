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

package docs.javadsl;

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.geode.GeodeSettings;
import org.apache.pekko.stream.connectors.geode.RegionSettings;
import org.apache.pekko.stream.connectors.geode.javadsl.Geode;
import org.apache.pekko.stream.connectors.geode.javadsl.GeodeWithPoolSubscription;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;

public class GeodeBaseTestCase {
  @Rule public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

  protected static final Logger LOGGER = LoggerFactory.getLogger(GeodeFlowTestCase.class);

  protected static ActorSystem system;
  private String geodeDockerHostname = "localhost";

  {
    String geodeItHostname = System.getenv("IT_GEODE_HOSTNAME");
    if (geodeItHostname != null) geodeDockerHostname = geodeItHostname;
  }

  // #region
  protected final RegionSettings<Integer, Person> personRegionSettings =
      RegionSettings.create("persons", Person::getId);
  protected final RegionSettings<Integer, Animal> animalRegionSettings =
      RegionSettings.create("animals", Animal::getId);
  // #region

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create();
  }

  static Source<Person, NotUsed> buildPersonsSource(Integer... ids) {
    return Source.from(Arrays.asList(ids))
        .map((i) -> new Person(i, String.format("Person Java %d", i), new Date()));
  }

  static Source<Animal, NotUsed> buildAnimalsSource(Integer... ids) {
    return Source.from(Arrays.asList(ids))
        .map((i) -> new Animal(i, String.format("Animal Java %d", i), 1));
  }

  protected Geode createGeodeClient() {
    String hostname = this.geodeDockerHostname;
    // #connection
    GeodeSettings settings =
        GeodeSettings.create(hostname, 10334).withConfiguration(c -> c.setPoolIdleTimeout(10));
    Geode geode = new Geode(settings);
    system.registerOnTermination(() -> geode.close());
    // #connection
    return geode;
  }

  protected GeodeWithPoolSubscription createGeodeWithPoolSubscription() {
    GeodeSettings settings = GeodeSettings.create(geodeDockerHostname, 10334);
    // #connection-with-pool
    GeodeWithPoolSubscription geode = new GeodeWithPoolSubscription(settings);
    // #connection-with-pool
    return geode;
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }
}
