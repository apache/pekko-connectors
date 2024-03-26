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

package org.apache.pekko.stream.connectors.ironmq.impl

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream._
import pekko.stream.connectors.ironmq._
import pekko.stream.stage._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Internal API.
 *
 * It is a very trivial IronMQ push stage. It push the message to IronMq as soon as they are pushed to this Stage.
 *
 * Because of that it does not guarantee the order of the produced messages and does not apply any backpressure. A more
 * sophisticated implementation will buffer the messages before pushing them and allow only a certain amount of parallel
 * requests.
 */
@InternalApi
private[ironmq] class IronMqPushStage(queueName: String, settings: IronMqSettings)
    extends GraphStage[FlowShape[PushMessage, Future[Message.Ids]]] {

  val in: Inlet[PushMessage] = Inlet("IronMqPush.in")
  val out: Outlet[Future[Message.Ids]] = Outlet("IronMqPush.out")

  override protected def initialAttributes: Attributes = Attributes.name("IronMqPush")

  override val shape: FlowShape[PushMessage, Future[Message.Ids]] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {

      implicit def ec: ExecutionContext = materializer.executionContext

      override protected val logSource: Class[_] = classOf[IronMqPushStage]

      private var runningFutures: Int = 0
      private var exceptionFromUpstream: Option[Throwable] = None
      private var client: IronMqClient = _ // set in preStart

      override def preStart(): Unit = {
        super.preStart()
        client = IronMqClient(settings)(materializer.system, materializer)
      }

      setHandler(
        in,
        new InHandler {

          override def onPush(): Unit = {
            val pushMessage = grab(in)

            val future = client.pushMessages(queueName, pushMessage)
            runningFutures = runningFutures + 1
            setKeepGoing(true)

            push(out, future)

            future.onComplete { _ =>
              futureCompleted.invoke(())
            }
          }

          override def onUpstreamFinish(): Unit =
            checkForCompletion()

          override def onUpstreamFailure(ex: Throwable): Unit = {
            exceptionFromUpstream = Some(ex)
            checkForCompletion()
          }
        })

      setHandler(out,
        new OutHandler {
          override def onPull(): Unit =
            tryPull(in)
        })

      private def checkForCompletion(): Unit =
        if (isClosed(in) && runningFutures <= 0)
          exceptionFromUpstream match {
            case None     => completeStage()
            case Some(ex) => failStage(ex)
          }

      private val futureCompleted = getAsyncCallback[Unit] { _ =>
        runningFutures = runningFutures - 1
        checkForCompletion()
      }

    }
}
