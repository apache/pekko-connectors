/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.influxdb.impl

import java.time.Instant
import java.util.concurrent.TimeUnit

import scala.jdk.CollectionConverters._

import docs.scaladsl.InfluxDbSourceCpu
import org.influxdb.dto.QueryResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PekkoConnectorsResultMapperHelperSpec extends AnyWordSpec with Matchers {

  "PekkoConnectorsResultMapperHelper" should {
    "map annotated fields with VarHandles" in {
      val helper = new PekkoConnectorsResultMapperHelper
      val series = new QueryResult.Series
      series.setColumns(List("time", "hostname", "region", "idle", "happydevop", "uptimesecs").asJava)
      series.setValues(
        List(
          List[AnyRef](
            "2026-07-06T09:42:00Z",
            "local_1",
            "eu-west-2",
            Double.box(1.5d),
            java.lang.Boolean.TRUE,
            Double.box(123d)).asJava).asJava)

      val parsed = helper.parseSeriesAs(classOf[InfluxDbSourceCpu], series, TimeUnit.MILLISECONDS)

      parsed should have size 1
      parsed.head.getTime shouldBe Instant.parse("2026-07-06T09:42:00Z")
      parsed.head.getHostname shouldBe "local_1"
      parsed.head.getRegion shouldBe "eu-west-2"
      parsed.head.getIdle shouldBe 1.5d
      parsed.head.getHappydevop shouldBe true
      parsed.head.getUptimeSecs shouldBe 123L
    }

    "convert annotated models to points with cached MethodHandles and VarHandles" in {
      val helper = new PekkoConnectorsResultMapperHelper
      val model =
        new InfluxDbSourceCpu(Instant.parse("2026-07-06T09:42:00Z"), "local_2", "eu-west-1", 2.5d, false, 125L)

      val lineProtocol = helper.convertModelToPoint(model).lineProtocol()

      lineProtocol should include("cpu")
      lineProtocol should include("hostname=local_2")
      lineProtocol should include("region=eu-west-1")
      lineProtocol should include("idle=2.5")
      lineProtocol should include("happydevop=false")
      lineProtocol should include("uptimesecs=125")
    }
  }
}
