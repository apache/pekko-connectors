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

package com.github.pjfanning.pekkohttpspi.testcontainers

import java.util.concurrent.{TimeUnit, TimeoutException}
import java.util.function.Predicate

import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.output.{OutputFrame, WaitingConsumer}
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy
import org.testcontainers.utility.LogUtils

/** This strategy is based on the container log "Ready." from Localstack. Once it's printed out, the container is good
  * to go.
  */
object LocalStackReadyLogWaitStrategy extends AbstractWaitStrategy {
  override def waitUntilReady(): Unit = {
    val waitingConsumer = new WaitingConsumer
    LogUtils.followOutput(DockerClientFactory.instance.client, waitStrategyTarget.getContainerId, waitingConsumer)

    val waitPredicate: Predicate[OutputFrame] = (outputFrame: OutputFrame) =>
      outputFrame.getUtf8String.contains("Ready.")

    try
      waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds, TimeUnit.SECONDS, 1)
    catch {
      case _: TimeoutException =>
        throw new ContainerLaunchException("Timed out waiting for localstack")
    }
  }
}
