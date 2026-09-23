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

package org.apache.pekko.stream.connectors.hbase.impl

import org.apache.pekko
import pekko.stream.{ Attributes, Outlet, SourceShape }
import pekko.stream.connectors.hbase.HTableSettings
import pekko.stream.stage.{ GraphStage, GraphStageLogic, OutHandler, StageLogging }
import org.apache.hadoop.hbase.client.{ Connection, Result, ResultScanner, Scan, Table }

import scala.util.control.NonFatal

private[hbase] final class HBaseSourceStage[A](scan: Scan, settings: HTableSettings[A])
    extends GraphStage[SourceShape[Result]] {

  val out: Outlet[Result] = Outlet("HBaseSource.out")
  override val shape: SourceShape[Result] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new HBaseSourceLogic[A](scan, settings, out, shape)
}

private[hbase] final class HBaseSourceLogic[A](scan: Scan,
    settings: HTableSettings[A],
    out: Outlet[Result],
    shape: SourceShape[Result])
    extends GraphStageLogic(shape)
    with OutHandler
    with StageLogging
    with HBaseCapabilities {

  implicit val connection: Connection = connect(settings.conf)

  lazy val table: Table = getOrCreateTable(settings.tableName, settings.columnFamilies).get
  // `table` is lazy, so postStop must not touch it unless preStart forced it —
  // referencing it there would create a table only to close it.
  private var tableOpened = false
  private var scanner: ResultScanner = null
  private var results: java.util.Iterator[Result] = null

  setHandler(out, this)

  override def preStart(): Unit =
    try {
      val t = table
      tableOpened = true
      scanner = t.getScanner(scan)
      results = scanner.iterator()
    } catch {
      case NonFatal(exc) =>
        failStage(exc)
    }

  // Every resource is closed independently, so a failure to close one still
  // releases the rest. Any of them may be unset if preStart failed part way.
  override def postStop(): Unit =
    HBaseSourceLogic.closeAll(
      if (scanner ne null) scanner.close(),
      if (tableOpened) table.close(),
      connection.close())((what, exc) => log.error(exc, "Problem occurred during {} close", what))

  override def onPull(): Unit =
    if (results.hasNext) {
      emit(out, results.next)
    } else {
      completeStage()
    }

}

private[impl] object HBaseSourceLogic {

  /**
   * Close the scanner, table and connection of a source stage. Each is closed
   * independently so that a failure to close one still releases the others.
   * Failures are reported to `onError` and not rethrown, matching the
   * behaviour of `HBaseFlowStage`: the stage has already stopped, so failing
   * here would only mask the reason it stopped.
   */
  def closeAll(closeScanner: => Unit, closeTable: => Unit, closeConnection: => Unit)(
      onError: (String, Throwable) => Unit): Unit = {
    def attempt(what: String, close: => Unit): Unit =
      try close
      catch {
        case NonFatal(exc) => onError(what, exc)
      }

    attempt("scanner", closeScanner)
    attempt("table", closeTable)
    attempt("connection", closeConnection)
  }
}
