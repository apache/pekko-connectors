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

import java.time.Instant;

import org.influxdb.annotation.Measurement;

@Measurement(name = "cpu", database = "InfluxDbSourceTest")
public class InfluxDbSourceCpu extends Cpu {

  public InfluxDbSourceCpu() {}

  public InfluxDbSourceCpu(
      Instant time,
      String hostname,
      String region,
      Double idle,
      Boolean happydevop,
      Long uptimeSecs) {
    super(time, hostname, region, idle, happydevop, uptimeSecs);
  }
}
