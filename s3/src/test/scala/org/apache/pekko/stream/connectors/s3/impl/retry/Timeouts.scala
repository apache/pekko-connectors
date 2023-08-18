/*
 * Copyright 2015 Johan Andrén
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.s3.impl.retry

import java.util.{ Timer, TimerTask }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.Try

// copied from https://github.com/johanandren/futiles/blob/18868f252bbf5dd71d2cd0fc67e7eb39863b686a/src/main/scala/markatta/futiles/Timeouts.scala
object Timeouts {

  private val timer = new Timer()

  /**
   * When ```waitFor``` has passed, evaluate ```what``` on the given execution context and complete the future
   */
  def timeout[A](waitFor: FiniteDuration)(what: => A)(implicit ec: ExecutionContext): Future[A] = {
    val promise = Promise[A]()
    timer.schedule(new TimerTask {
        override def run(): Unit =
          // make sure we do not block the timer thread
          Future {
            promise.complete(Try(what))
          }
      },
      waitFor.toMillis)

    promise.future
  }
}
