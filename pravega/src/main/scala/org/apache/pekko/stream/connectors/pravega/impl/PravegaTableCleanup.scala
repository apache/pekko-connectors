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

package org.apache.pekko.stream.connectors.pravega.impl

import org.apache.pekko
import pekko.annotation.InternalApi

import scala.util.control.NonFatal

/**
 * INTERNAL API
 *
 * Shared cleanup for the stages that open a `KeyValueTableFactory` and a
 * `KeyValueTable` from it.
 */
@InternalApi private[pravega] object PravegaTableCleanup {

  /**
   * Close the table and then the factory.
   *
   * `preStart` creates the factory first and the table from it, so a failure in
   * between leaves the factory open and the table unset. Both are therefore
   * closed defensively and independently: the factory owns the connection pool
   * and has to be released even when closing the table fails or was never
   * needed.
   *
   * Failures are reported to `onError` and not rethrown; the stage has already
   * stopped, so failing here would only mask why it stopped.
   */
  def closeTableAndFactory(closeTable: => Unit, closeFactory: => Unit)(
      onError: (String, Throwable) => Unit): Unit = {
    def attempt(what: String, close: => Unit): Unit =
      try close
      catch {
        case NonFatal(exc) => onError(what, exc)
      }

    attempt("table", closeTable)
    attempt("key value table factory", closeFactory)
  }
}
