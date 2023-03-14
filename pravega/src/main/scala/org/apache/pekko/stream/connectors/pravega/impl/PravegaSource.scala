/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.pravega.impl

import org.apache.pekko.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, OutHandler, StageLogging }
import org.apache.pekko.stream.{ Attributes, Outlet, SourceShape }
import org.apache.pekko.Done
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.event.Logging
import org.apache.pekko.stream.connectors.pravega.{ PravegaEvent, ReaderSettings }
import io.pravega.client.ClientConfig

import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration.DurationLong
import io.pravega.client.stream.{ EventStreamReader, ReaderGroup }

import scala.util.control.NonFatal
import org.apache.pekko.stream.ActorAttributes
import org.apache.pekko.stream.stage.AsyncCallback

import java.util.UUID
import scala.util.{ Failure, Success, Try }

@InternalApi private final class PravegaSourcesStageLogic[A](
    shape: SourceShape[PravegaEvent[A]],
    readerGroup: ReaderGroup,
    val readerSettings: ReaderSettings[A],
    startupPromise: Promise[Done]) extends GraphStageLogic(shape)
    with PravegaCapabilities
    with StageLogging {

  protected val scope = readerGroup.getScope

  override protected def logSource = classOf[PravegaSourcesStageLogic[A]]

  private def out: Outlet[PravegaEvent[A]] = shape.out

  private var reader: EventStreamReader[A] = _

  protected val clientConfig: ClientConfig = readerSettings.clientConfig

  private val asyncOnPull: AsyncCallback[OutHandler] = getAsyncCallback { out =>
    out.onPull()
  }

  setHandler(
    out,
    new OutHandler {

      override def onPull(): Unit = {
        val eventRead = reader.readNextEvent(readerSettings.timeout)
        if (eventRead.isCheckpoint) {
          log.debug("Checkpoint: {}", eventRead.getCheckpointName)
          onPull()
        } else {
          val event = eventRead.getEvent
          if (event == null) {
            log.debug("a timeout occurred while waiting for new messages")
            materializer.scheduleOnce(readerSettings.timeout.millis, () => asyncOnPull.invoke(this))
          } else
            push(out, new PravegaEvent(event, eventRead.getPosition, eventRead.getEventPointer))
        }
      }

    })

  override def preStart(): Unit = {
    log.debug("Start consuming {}...", readerGroup.toString)
    try {
      reader = createReader(readerSettings, readerGroup)
      startupPromise.success(Done)
    } catch {
      case NonFatal(exception) =>
        log.error(exception.getMessage())
        failStage(exception)
    }
  }

  private def createReader(settings: ReaderSettings[A], readerGroup: ReaderGroup): EventStreamReader[A] =
    eventStreamClientFactory.createReader(
      settings.readerId.getOrElse(UUID.randomUUID().toString),
      readerGroup.getGroupName,
      settings.serializer,
      settings.readerConfig)

  override def postStop(): Unit = {
    log.debug("Stopping reader")
    Try(reader.close()) match {
      case Failure(exception) =>
        log.error(exception, s"Error while closing [{}/{}]", scope, readerGroup.toString)
      case Success(_) =>
        log.debug("Closed reader [{}/{}]", scope, readerGroup.getGroupName)
    }
    Try(readerGroup.close()) match {
      case Failure(exception) =>
        log.error(exception, s"Error while closing reader group [{}/{}]", scope, readerGroup.getGroupName)
      case Success(_) =>
        log.debug("Closed reader group [{}/{}]", scope, readerGroup.getGroupName)
    }
    close()
  }

}

@InternalApi private[pravega] final class PravegaSource[A](
    readerGroup: ReaderGroup,
    settings: ReaderSettings[A]) extends GraphStageWithMaterializedValue[SourceShape[PravegaEvent[A]], Future[Done]] {

  private val out: Outlet[PravegaEvent[A]] = Outlet(Logging.simpleName(this) + ".out")

  override val shape: SourceShape[PravegaEvent[A]] = SourceShape(out)

  override protected def initialAttributes: Attributes =
    super.initialAttributes and Attributes.name(Logging.simpleName(this)) and ActorAttributes.IODispatcher

  override def createLogicAndMaterializedValue(
      inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val startupPromise = Promise[Done]()

    val logic = new PravegaSourcesStageLogic[A](
      shape,
      readerGroup,
      settings,
      startupPromise)

    (logic, startupPromise.future)

  }

}
