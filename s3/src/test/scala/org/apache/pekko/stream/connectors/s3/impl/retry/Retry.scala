/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

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

import java.util.concurrent.{ ThreadLocalRandom, TimeUnit }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

// copied from https://github.com/johanandren/futiles/blob/18868f252bbf5dd71d2cd0fc67e7eb39863b686a/src/main/scala/markatta/futiles/Retry.scala
object Retry {

  private val alwaysRetry: Throwable => Boolean = _ => true

  /**
   * Evaluate a block that creates a future up to a specific number of times, if the future fails, decide about
   * retrying using a predicate, if it should retry an exponential back off is applied so that the retry waits longer
   * and longer for every retry it makes. A jitter is also added so that the exact timing of the retry isn't exactly
   * the same for all calls with the same backOffUnit
   *
   * Any exception in the block creating the future will also be returned as a failed future Default is to retry for
   * all throwables.
   *
   * Based on this wikipedia article: http://en.wikipedia.org/wiki/Truncated_binary_exponential_backoff
   */
  def retryWithBackOff[A](
      times: Int,
      backOffUnit: FiniteDuration,
      shouldRetry: Throwable => Boolean = alwaysRetry)(fBlock: => Future[A])(implicit ec: ExecutionContext): Future[A] =
    try
      if (times <= 1) fBlock
      else retryWithBackOffLoop(times, 1, backOffUnit, shouldRetry)(fBlock)
    catch {
      // failure to actually create the future
      case x: Throwable => Future.failed(x)
    }

  private def retryWithBackOffLoop[A](
      totalTimes: Int,
      timesTried: Int,
      backOffUnit: FiniteDuration,
      shouldRetry: Throwable => Boolean)(fBlock: => Future[A])(implicit ec: ExecutionContext): Future[A] =
    if (totalTimes <= timesTried) fBlock
    else
      fBlock.recoverWith {
        case ex: Throwable if shouldRetry(ex) =>
          val timesTriedNow = timesTried + 1
          val backOff = nextBackOff(timesTriedNow, backOffUnit)
          Timeouts
            .timeout(backOff)(())
            .flatMap(_ =>
              retryWithBackOffLoop(
                totalTimes,
                timesTriedNow,
                backOffUnit,
                shouldRetry)(fBlock))
      }

  private def nextBackOff(
      tries: Int,
      backOffUnit: FiniteDuration): FiniteDuration = {
    require(tries > 0, "tries should start from 1")
    val rng = new Random(ThreadLocalRandom.current())
    // jitter between 0.5 and 1.5
    val jitter = 0.5 + rng.nextDouble()
    val factor = math.pow(2, tries) * jitter
    FiniteDuration(
      (backOffUnit.toMillis * factor).toLong,
      TimeUnit.MILLISECONDS)
  }

}
