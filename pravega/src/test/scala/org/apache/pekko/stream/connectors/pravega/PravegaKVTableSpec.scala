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

package org.apache.pekko.stream.connectors.pravega

import org.apache.pekko
import pekko.stream.scaladsl.{ Keep, Sink, Source }

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Await
import scala.concurrent.duration._

import pekko.stream.connectors.testkit.scaladsl.Repeated
import pekko.stream.testkit.scaladsl.TestSink
import pekko.stream.connectors.pravega.impl.PravegaTableSource
import pekko.stream.connectors.pravega.scaladsl.PravegaTable
import io.pravega.client.tables.TableKey
import org.scalatest.concurrent.Eventually

import docs.scaladsl.Person
import docs.scaladsl.Serializers._

class PravegaKVTableSpec extends PravegaBaseSpec with Repeated with Eventually {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 30.seconds, interval = 200.millis)

  private val tablewriterSettings: TableWriterSettings[Int, Person] =
    TableWriterSettingsBuilder[Int, Person]()
      .withKeyExtractor(id => new TableKey(intSerializer.serialize(id)))
      .build()

  "Pravega connector" should {

    "write and read in KVP table with keyFamily = \"test\" " in {

      val scope = newScope()

      val tableName = "kvp-table-name"

      createTable(scope, tableName, 4)

      val sink = PravegaTable.sink(scope, tableName, tablewriterSettings)

      val fut = Source(1 to 100)
        .map(id => (id, Person(id, s"name_$id")))
        .runWith(sink)

      Await.ready(fut, remainingOrDefault)

      val tableSettings = TableReaderSettingsBuilder[Int, Person]()
        .withKeyExtractor(id => new TableKey(intSerializer.serialize(id)))
        .build()

      val readingDone = PravegaTable
        .source(scope, tableName, tableSettings)
        .toMat(Sink.fold(0) { (sum, value) =>
          sum + 1
        })(Keep.right)
        .run()

      whenReady(readingDone) { sum =>
        logger.info(s"Sum: $sum")
        sum mustEqual 100
      }

    }

    "fail the materialized future of the table source when the table cannot be opened" in {
      val scope = newScope()
      createTable(scope, "existing-table", 4)

      val tableSettings = TableReaderSettingsBuilder[Int, Person]()
        .withKeyExtractor(id => new TableKey(intSerializer.serialize(id)))
        .build()

      val (started, done) = PravegaTable
        .source(scope, "missing-table", tableSettings)
        .toMat(Sink.ignore)(Keep.both)
        .run()

      // the stream fails with the error that stopped preStart, not with a NullPointerException from postStop
      val streamFailure = done.failed.futureValue
      streamFailure must not be a[NullPointerException]
      started.failed.futureValue mustBe streamFailure
    }

    "fail the read flow with the original error when the table cannot be opened" in {
      val scope = newScope()
      createTable(scope, "existing-table", 4)

      val tableSettings = TableReaderSettingsBuilder[Int, Person]()
        .withKeyExtractor(id => new TableKey(intSerializer.serialize(id)))
        .build()

      val done = Source(1 to 10)
        .via(PravegaTable.readFlow(scope, "missing-table", tableSettings))
        .runWith(Sink.ignore)

      done.failed.futureValue must not be a[NullPointerException]
    }

    "release the Pravega client thread promptly when the table source is cancelled early" in {
      val scope = newScope()
      val tableName = "kvp-table-cancelled"
      createTable(scope, tableName, 4)

      Await.ready(
        Source(1 to 100)
          .map(id => (id, Person(id, s"name_$id")))
          .runWith(PravegaTable.sink(scope, tableName, tablewriterSettings)),
        remainingOrDefault)

      // far fewer permits than entries, so the iteration is parked waiting for demand when downstream cancels
      val tableSettings = TableReaderSettingsBuilder[Int, Person]()
        .withKeyExtractor(id => new TableKey(intSerializer.serialize(id)))
        .withMaximumInflightMessages(2)
        .build()

      // the gate of this materialization only: it counts the calls of the iteration callback
      val gate = new ObservedSemaphore(tableSettings.maximumInflightMessages)

      val probe = Source
        .fromGraph(new PravegaTableSource[Int, Person](scope, tableName, tableSettings, _ => gate))
        .runWith(TestSink[TableEntry[Person]]())
      probe.requestNext()

      // the callback is waiting in acquire on the exhausted gate, as no further element is requested
      eventually {
        gate.hasQueuedThreads mustBe true
        gate.availablePermits mustBe 0
      }
      val acquireCalls = gate.acquireCalls
      gate.acquireReturns mustBe acquireCalls - 1

      // without the wake-up in postStop the callback stays in acquire after the stage stopped
      probe.cancel()
      eventually(timeout(3.seconds)) {
        gate.acquireReturns mustBe acquireCalls
        gate.hasQueuedThreads mustBe false
      }
      // the callback exits: it used the permit of the wake-up and does not ask for another one
      gate.acquireCalls mustBe acquireCalls
      gate.availablePermits mustBe 0
    }

  }

}

/** Counts the calls to `acquire`, which only the iteration callback of the table source makes. */
private final class ObservedSemaphore(permits: Int) extends Semaphore(permits) {
  private val entered = new AtomicInteger
  private val returned = new AtomicInteger

  def acquireCalls: Int = entered.get
  def acquireReturns: Int = returned.get

  override def acquire(): Unit = {
    entered.incrementAndGet()
    try super.acquire()
    finally returned.incrementAndGet()
  }
}
