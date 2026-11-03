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

package org.apache.pekko.stream.connectors.cassandra.javadsl

import java.util.concurrent.{ CompletionStage, Executor }

import org.apache.pekko
import pekko.stream.connectors.cassandra.CqlSessionProvider
import com.datastax.oss.driver.api.core.CqlSession

import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.FutureConverters._

/**
 * Java API: base class for implementing a [[pekko.stream.connectors.cassandra.CqlSessionProvider]] in Java,
 * using a `java.util.concurrent.Executor` and returning a `CompletionStage`.
 *
 * Like any `CqlSessionProvider`, an implementation can be configured with the `session-provider` property
 * and may optionally have a constructor with an `ActorSystem` and `Config` parameter.
 *
 * @since 2.0.0
 */
abstract class AbstractCqlSessionProvider extends CqlSessionProvider {

  /**
   * Create the `CqlSession`.
   *
   * @param executor executor to use for asynchronous callbacks
   */
  def connect(executor: Executor): CompletionStage[CqlSession]

  final override def connect()(implicit ec: ExecutionContext): Future[CqlSession] = {
    val executor: Executor = ec match {
      case e: Executor => e
      case _           => (runnable: Runnable) => ec.execute(runnable)
    }
    connect(executor).asScala
  }
}
