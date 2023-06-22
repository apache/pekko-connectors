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
import pekko.stream._
import pekko.stream.connectors.hbase.HTableSettings
import pekko.stream.stage._
import org.apache.hadoop.hbase.client.{ Attributes => _, _ }

import scala.util.control.NonFatal

private[hbase] class HBaseFlowStage[A](settings: HTableSettings[A]) extends GraphStage[FlowShape[A, A]] {

  override protected def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name("HBaseFlow") and ActorAttributes.IODispatcher

  private val in = Inlet[A]("messages")
  private val out = Outlet[A]("result")

  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging with HBaseCapabilities {

      override protected def logSource = classOf[HBaseFlowStage[A]]

      implicit val connection = connect(settings.conf)

      lazy val table: Table = getOrCreateTable(settings.tableName, settings.columnFamilies).get

      setHandler(out,
        new OutHandler {
          override def onPull() =
            pull(in)
        })

      setHandler(
        in,
        new InHandler {
          override def onPush() = {
            val msg = grab(in)

            val mutations = settings.converter(msg)

            for (mutation <- mutations) {
              mutation match {
                case x: Put       => table.put(x)
                case x: Delete    => table.delete(x)
                case x: Append    => table.append(x)
                case x: Increment => table.increment(x)
                case _            =>
              }
            }

            push(out, msg)
          }

        })

      override def postStop() = {
        log.debug("Stage completed")
        try {
          table.close()
          log.debug("table closed")
        } catch {
          case NonFatal(ex) => log.error(ex, "Problem occurred during producer table close")
        }
        try {
          connection.close()
          log.debug("connection closed")
        } catch {
          case NonFatal(ex) => log.error(ex, "Problem occurred during producer connection close")
        }
      }
    }

}
