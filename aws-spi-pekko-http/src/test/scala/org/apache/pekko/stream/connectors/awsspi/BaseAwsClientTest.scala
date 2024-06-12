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

package org.apache.pekko.stream.connectors.awsspi

import java.net.URI

import org.apache.pekko
import pekko.stream.connectors.awsspi.testcontainers.LocalStackReadyLogWaitStrategy
import com.dimafeng.testcontainers.{ ForAllTestContainer, GenericContainer }
import org.scalatest.concurrent.{ Eventually, Futures, IntegrationPatience }
import org.scalatest.BeforeAndAfter
import software.amazon.awssdk.core.SdkClient
import software.amazon.awssdk.regions.Region

import scala.util.Random
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

trait BaseAwsClientTest[C <: SdkClient]
    extends AnyWordSpec
    with Matchers
    with Futures
    with Eventually
    with BeforeAndAfter
    with IntegrationPatience
    with ForAllTestContainer {

  lazy val defaultRegion: Region = Region.EU_WEST_1

  def exposedServicePort: Int
  def container: GenericContainer

  def endpoint = new URI(s"http://localhost:${container.mappedPort(exposedServicePort)}")
  def randomIdentifier(length: Int): String = Random.alphanumeric.take(length).mkString
}

trait LocalstackBaseAwsClientTest[C <: SdkClient] extends BaseAwsClientTest[C] {
  def service: String

  lazy val exposedServicePort: Int = 4566

  override lazy val container: GenericContainer =
    new GenericContainer(
      dockerImage = "localstack/localstack",
      exposedPorts = Seq(exposedServicePort),
      env = Map("SERVICES" -> service),
      waitStrategy = Some(LocalStackReadyLogWaitStrategy))
}

trait ElasticMQSQSBaseAwsClientTest[C <: SdkClient] extends BaseAwsClientTest[C] {
  def service: String

  lazy val exposedServicePort: Int = 9324

  override lazy val container: GenericContainer =
    new GenericContainer(
      dockerImage = "softwaremill/elasticmq-native",
      exposedPorts = Seq(exposedServicePort))
}
