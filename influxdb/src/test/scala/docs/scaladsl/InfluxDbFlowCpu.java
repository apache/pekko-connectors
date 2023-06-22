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

package docs.scaladsl;

import java.time.Instant;

import org.influxdb.annotation.Measurement;

import docs.javadsl.Cpu;

@Measurement(name = "cpu", database = "FlowSpec")
public class InfluxDbFlowCpu extends Cpu {

    public InfluxDbFlowCpu() {
    }

    public InfluxDbFlowCpu(Instant time, String hostname, String region, Double idle, Boolean happydevop, Long uptimeSecs) {
        super(time, hostname, region, idle, happydevop, uptimeSecs);
    }

    public InfluxDbFlowCpu cloneAt(Instant time) {
        return new InfluxDbFlowCpu(time, getHostname(), getRegion(), getIdle(), getHappydevop(), getUptimeSecs());
    }

}

