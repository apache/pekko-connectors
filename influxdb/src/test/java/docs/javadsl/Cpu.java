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
import org.influxdb.annotation.Column;

public class Cpu {

  @Column(name = "time")
  private Instant time;

  @Column(name = "hostname", tag = true)
  private String hostname;

  @Column(name = "region", tag = true)
  private String region;

  @Column(name = "idle")
  private Double idle;

  @Column(name = "happydevop")
  private Boolean happydevop;

  @Column(name = "uptimesecs")
  private Long uptimeSecs;

  public Cpu() {}

  public Cpu(
      Instant time,
      String hostname,
      String region,
      Double idle,
      Boolean happydevop,
      Long uptimeSecs) {
    this.time = time;
    this.hostname = hostname;
    this.region = region;
    this.idle = idle;
    this.happydevop = happydevop;
    this.uptimeSecs = uptimeSecs;
  }

  public Instant getTime() {
    return time;
  }

  public String getHostname() {
    return hostname;
  }

  public String getRegion() {
    return region;
  }

  public Double getIdle() {
    return idle;
  }

  public Boolean getHappydevop() {
    return happydevop;
  }

  public Long getUptimeSecs() {
    return uptimeSecs;
  }
}
