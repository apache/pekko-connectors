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

import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProviderChain,
  EnvironmentVariableCredentialsProvider,
  ProfileCredentialsProvider,
  SystemPropertyCredentialsProvider
}
import software.amazon.awssdk.regions.Region

import scala.util.Random

trait TestBase {

  private lazy val credentialProfileName = "spi-test-account"

  lazy val defaultRegion = Region.EU_WEST_1

  def randomIdentifier(length: Int): String = Random.alphanumeric.take(length).mkString

  lazy val credentialProviderChain =
    AwsCredentialsProviderChain.builder
      .credentialsProviders(
        EnvironmentVariableCredentialsProvider.create,
        SystemPropertyCredentialsProvider.create,
        ProfileCredentialsProvider.builder
          .profileName(credentialProfileName)
          .build)
      .build
}
