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

package org.apache.pekko.stream.connectors.s3

import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import java.time.Duration

class MinioContainer(accessKey: String, secretKey: String, domain: String)
    extends GenericContainer(
      "minio/minio:RELEASE.2023-04-13T03-08-07Z",
      exposedPorts = List(9000),
      waitStrategy = Some(Wait.forHttp("/minio/health/ready").forPort(9000).withStartupTimeout(Duration.ofSeconds(10))),
      command = List("server", "/data"),
      env = Map(
        "MINIO_ACCESS_KEY" -> accessKey,
        "MINIO_SECRET_KEY" -> secretKey,
        "MINIO_DOMAIN" -> domain)) {

  def getHostAddress: String =
    s"http://${container.getHost}:${container.getMappedPort(9000)}"

  def getVirtualHost: String =
    s"http://{bucket}.$domain:${container.getMappedPort(9000)}"

}
