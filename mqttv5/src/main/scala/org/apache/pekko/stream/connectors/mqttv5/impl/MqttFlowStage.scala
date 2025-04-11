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

package org.apache.pekko.stream.connectors.mqttv5.impl

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal

import org.apache.pekko.Done
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.Shape
import org.apache.pekko.stream._
import org.apache.pekko.stream.connectors.mqttv5.AuthSettings
import org.apache.pekko.stream.connectors.mqttv5.MqttConnectionSettings
import org.apache.pekko.stream.connectors.mqttv5.MqttMessage
import org.apache.pekko.stream.connectors.mqttv5.MqttOfflinePersistenceSettings
import org.apache.pekko.stream.connectors.mqttv5.MqttQoS
import org.apache.pekko.stream.connectors.mqttv5.scaladsl.MqttMessageWithAck
import org.apache.pekko.stream.stage._
import org.apache.pekko.util.ByteString
import org.eclipse.paho.mqttv5.client.DisconnectedBufferOptions
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode
import org.eclipse.paho.mqttv5.common.{ MqttMessage => PahoMqttMessage }

/**
 * INTERNAL API
 */
@InternalApi
private[mqttv5] final class MqttFlowStage(
    connectionSettings: MqttConnectionSettings,
    subscriptions: Map[String, MqttQoS],
    bufferSize: Int,
    defaultQoS: MqttQoS,
    manualAcks: Boolean = false
) extends GraphStageWithMaterializedValue[FlowShape[MqttMessage, MqttMessageWithAck], Future[Done]] {

  private val in = Inlet[MqttMessage]("MqttFlow.in")
  private val out = Outlet[MqttMessageWithAck]("MqttFlow.out")
  override val shape: Shape = FlowShape(in, out)

  override protected def initialAttributes: Attributes = Attributes.name("MqttFlow")

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val subscriptionPromise = Promise[Done]()
    val logic = new MqttFlowStageLogic[MqttMessage](
      in = in,
      out = out,
      shape = shape,
      subscriptionPromise = subscriptionPromise,
      connectionSettings = connectionSettings,
      subscriptions = subscriptions,
      bufferSize = bufferSize,
      defaultQoS = defaultQoS,
      manualAcks = manualAcks
    ) {
      override def publishPending(msg: MqttMessage): Unit = super.publishToMqtt(msg)
    }
    (logic, subscriptionPromise.future)
  }
}

abstract class MqttFlowStageLogic[I](
    in: Inlet[I],
    out: Outlet[MqttMessageWithAck],
    shape: Shape,
    subscriptionPromise: Promise[Done],
    connectionSettings: MqttConnectionSettings,
    subscriptions: Map[String, MqttQoS],
    bufferSize: Int,
    defaultQoS: MqttQoS,
    manualAcks: Boolean
) extends GraphStageLogic(shape)
    with StageLogging
    with InHandler
    with OutHandler {

  import MqttFlowStageLogic._

  private val backpressurePahoClient = new Semaphore(bufferSize)
  private var pendingMsg = Option.empty[I]
  private val queue = mutable.Queue[MqttMessageWithAck]()
  private val unackedMessages = new AtomicInteger()

  protected def handleDeliveryComplete(token: IMqttToken): Unit = ()

  private val onSubscribe: AsyncCallback[Try[IMqttToken]] = getAsyncCallback[Try[IMqttToken]] { conn =>
    if (subscriptionPromise.isCompleted) {
      log.debug(
        "Client [{}] re-established subscription to broker [{}]",
        connectionSettings.clientId,
        connectionSettings.broker
      )
    } else {
      subscriptionPromise.complete(conn.map(_ => {
        log.debug(
          "Client [{}] established subscription to broker [{}]",
          connectionSettings.clientId,
          connectionSettings.broker
        )
        Done
      }))
      pull(in)
    }
  }

  private val onConnect: AsyncCallback[IMqttAsyncClient] =
    getAsyncCallback[IMqttAsyncClient]((client: IMqttAsyncClient) => {
      if (subscriptions.nonEmpty) {
        if (manualAcks) client.setManualAcks(true)
        val (topics, qoses) = subscriptions.unzip
        log.debug(
          "Client [{}] connected to broker [{}]; subscribing to [{}]",
          connectionSettings.clientId,
          connectionSettings.broker,
          subscriptions.map(sub => s"${sub._1}(qos=${sub._2.value})").mkString(", ")
        )
        client.subscribe(
          topics.toArray,
          qoses.map(_.value).toArray,
          /* userContext */ null,
          /* callback */ new MqttActionListener {
            def onSuccess(token: IMqttToken): Unit = {
              token.getReasonCodes.toList.filter(_ >= MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR).distinct match {
                case Nil =>
                  onSubscribe.invoke(Success(token))

                case errors =>
                  val message = s"Client [${connectionSettings.clientId}] received one or more errors " +
                    s"while subscribing to broker [${connectionSettings.broker}]: " +
                    s"[${errors.map(e => s"code=${e.toString}").mkString(",")}]"
                  log.error(message)
                  onSubscribe.invoke(Failure(new RuntimeException(message)))
              }
            }

            def onFailure(token: IMqttToken, ex: Throwable): Unit =
              onSubscribe.invoke(Failure(ex))
          }
        )
      } else {
        log.debug(
          "Client [{}] connected to broker [{}] without subscriptions",
          connectionSettings.clientId,
          connectionSettings.broker
        )
        subscriptionPromise.complete(SuccessfullyDone)
        pull(in)
      }
    })

  private val onConnectionLost: AsyncCallback[Throwable] = getAsyncCallback[Throwable](failStageWith)

  private val onMessageAsyncCallback: AsyncCallback[MqttMessageWithAck] =
    getAsyncCallback[MqttMessageWithAck] { message =>
      if (isAvailable(out)) {
        pushDownstream(message)
      } else if (queue.size + 1 > bufferSize) {
        failStageWith(new RuntimeException(s"Reached maximum buffer size [$bufferSize]"))
      } else {
        queue.enqueue(message)
      }
    }

  private val onPublished: AsyncCallback[Try[IMqttToken]] = getAsyncCallback[Try[IMqttToken]] {
    case Success(_)  => if (!hasBeenPulled(in)) pull(in)
    case Failure(ex) => failStageWith(ex)
  }

  private def createPahoBufferOptions(settings: MqttOfflinePersistenceSettings): DisconnectedBufferOptions = {
    val disconnectedBufferOptions = new DisconnectedBufferOptions()

    disconnectedBufferOptions.setBufferEnabled(true)
    disconnectedBufferOptions.setBufferSize(settings.bufferSize)
    disconnectedBufferOptions.setDeleteOldestMessages(settings.deleteOldestMessage)
    disconnectedBufferOptions.setPersistBuffer(settings.persistBuffer)

    disconnectedBufferOptions
  }

  private val client = new MqttAsyncClient(
    connectionSettings.broker,
    connectionSettings.clientId,
    connectionSettings.persistence
  )

  private def mqttClient: MqttAsyncClient = connectionSettings.offlinePersistence match {
    case Some(bufferOpts) =>
      client.setBufferOpts(createPahoBufferOptions(bufferOpts))
      client

    case _ =>
      client
  }

  private val commitCallback: AsyncCallback[CommitCallbackArguments] =
    getAsyncCallback[CommitCallbackArguments]((args: CommitCallbackArguments) =>
      try {
        mqttClient.messageArrivedComplete(args.messageId, args.qos.value)
        if (unackedMessages.decrementAndGet() == 0 && (isClosed(out) || (isClosed(in) && queue.isEmpty)))
          completeStage()
        args.promise.complete(SuccessfullyDone)
      } catch {
        case ex: Throwable => args.promise.failure(ex)
      }
    )

  mqttClient.setCallback(
    new MqttCallback {
      override def messageArrived(topic: String, pahoMessage: PahoMqttMessage): Unit = {
        backpressurePahoClient.acquire()
        val message = new MqttMessageWithAck {
          override val message: MqttMessage = MqttMessage(topic, ByteString.fromArrayUnsafe(pahoMessage.getPayload))

          override def ack(): Future[Done] = {
            val promise = Promise[Done]()
            val qos = pahoMessage.getQos match {
              case 0 => MqttQoS.AtMostOnce
              case 1 => MqttQoS.AtLeastOnce
              case 2 => MqttQoS.ExactlyOnce
            }
            commitCallback.invoke(CommitCallbackArguments(pahoMessage.getId, qos, promise))
            promise.future
          }
        }
        onMessageAsyncCallback.invoke(message)
      }

      override def deliveryComplete(token: IMqttToken): Unit =
        handleDeliveryComplete(token)

      override def disconnected(disconnectResponse: MqttDisconnectResponse): Unit = {
        if (!connectionSettings.automaticReconnect) {
          log.error(
            "Client [{}] lost connection to broker [{}] with [code={},reason={}]; " +
            "(hint: `automaticReconnect` can be enabled in `MqttConnectionSettings`)",
            connectionSettings.clientId,
            connectionSettings.broker,
            disconnectResponse.getReturnCode,
            disconnectResponse.getReasonString
          )
          onConnectionLost.invoke(disconnectResponse.getException)
        } else {
          log.warning(
            "Client [{}] lost connection to broker [{}] with [code={},reason={}]; trying to reconnect...",
            connectionSettings.clientId,
            connectionSettings.broker,
            disconnectResponse.getReturnCode,
            disconnectResponse.getReasonString
          )
        }
      }

      override def mqttErrorOccurred(exception: MqttException): Unit =
        failStageWith(exception)

      override def authPacketArrived(reasonCode: Int, properties: MqttProperties): Unit = {
        connectionSettings.auth match {
          case AuthSettings.Enhanced(_, _, _) if reasonCode == 0x00 =>
            // (re)authentication successful; no further action needed
            log.debug(
              "Authentication for client [{}] completed successfully with [codes={},reason={}]",
              connectionSettings.clientId,
              reasonCode,
              properties.getReasonString
            )

          case AuthSettings.Enhanced(_, _, authPacketHandler) if reasonCode == 0x18 =>
            // continue authentication
            log.debug(
              "Authentication for client [{}] continuing with [codes={},reason={}]",
              connectionSettings.clientId,
              reasonCode,
              properties.getReasonString
            )

            val (responseCode, responseProperties) = authPacketHandler(reasonCode, properties)

            val result = mqttClient.authenticate(
              /* reasonCode */ responseCode,
              /* userContext */ null,
              /* properties */ responseProperties
            )

            Option(result).foreach { token =>
              // the API docs say that a token for the operation is returned but the current
              // implementation in `org.eclipse.paho.mqttv5.client.MqttAsyncClient` actually
              // returns `null`; in case this changes in the future, here we log the results
              // of the operation and hope for the best
              token.setActionCallback(
                new MqttActionListener {
                  override def onSuccess(token: IMqttToken): Unit =
                    log.debug(
                      "Authentication call for client [{}] completed with [codes={},reason={}]",
                      connectionSettings.clientId,
                      token.getReasonCodes.distinct.sorted.mkString(";"),
                      token.getResponseProperties.getReasonString
                    )

                  override def onFailure(token: IMqttToken, ex: Throwable): Unit =
                    log.debug(
                      "Authentication call for client [{}] failed with [codes={},reason={}]: [{}]",
                      connectionSettings.clientId,
                      token.getReasonCodes.distinct.sorted.mkString(";"),
                      token.getResponseProperties.getReasonString,
                      s"${ex.getClass.getSimpleName} - ${ex.getMessage}"
                    )
                }
              )
            }

          case other =>
            // unexpected reason code received (if enhanced authentication is used)
            // OR
            // unexpected AUTH packet received (if no or simple authentication is used)
            log.warning(
              "Client [{}] with authentication type [{}] received an unexpected AUTH packet with [code={},reason={}]",
              connectionSettings.clientId,
              other.getClass.getSimpleName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase,
              reasonCode,
              properties.getReasonString
            )
        }
      }

      override def connectComplete(reconnect: Boolean, serverURI: String): Unit = {
        log.debug(
          "Connection completed for client [{}] with [reconnect={},serverURI={}]",
          connectionSettings.clientId,
          reconnect,
          serverURI
        )
        pendingMsg.foreach { msg =>
          log.debug("Client [{}] sending pending message to broker [{}]", connectionSettings.clientId, serverURI)
          publishPending(msg)
          pendingMsg = None
        }
        if (reconnect && !hasBeenPulled(in)) pull(in)
      }
    }
  )

  override def onPush(): Unit = {
    val msg = grab(in)
    try {
      publishPending(msg)
    } catch {
      case _: MqttException if connectionSettings.automaticReconnect => pendingMsg = Some(msg)
      case NonFatal(e)                                               => throw e
    }
  }

  override def onUpstreamFinish(): Unit = {
    setKeepGoing(true)
    if (queue.isEmpty && unackedMessages.get() == 0) super.onUpstreamFinish()
  }

  override def onUpstreamFailure(ex: Throwable): Unit = {
    setKeepGoing(true)
    if (queue.isEmpty && unackedMessages.get() == 0) super.onUpstreamFailure(ex)
  }

  override def onPull(): Unit =
    if (queue.nonEmpty) {
      pushDownstream(queue.dequeue())
      if (unackedMessages.get() == 0 && isClosed(in)) completeStage()
    }

  override def onDownstreamFinish(cause: Throwable): Unit = {
    setKeepGoing(true)
    if (unackedMessages.get() == 0) super.onDownstreamFinish(cause)
  }

  setHandlers(in, out, this)

  def publishToMqtt(msg: MqttMessage): IMqttToken = {
    val pahoMsg = new PahoMqttMessage(msg.payload.toArray)
    pahoMsg.setQos(msg.qos.getOrElse(defaultQoS).value)
    pahoMsg.setRetained(msg.retained)

    mqttClient.publish(
      msg.topic,
      pahoMsg,
      msg,
      /* callback */ new MqttActionListener {
        def onSuccess(token: IMqttToken): Unit = {
          token.getReasonCodes.toList.filter(_ >= MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR).distinct match {
            case Nil =>
              onPublished.invoke(Success(token))

            case errors =>
              val message = s"Client [${connectionSettings.clientId}] received one or more error codes " +
                s"while publishing on topic [${msg.topic}] to broker [${connectionSettings.broker}]: " +
                s"[${errors.map(e => s"code=${e.toString}").mkString(",")}]"
              log.error(message)
              onPublished.invoke(Failure(new RuntimeException(message)))
          }
        }

        def onFailure(token: IMqttToken, ex: Throwable): Unit =
          onPublished.invoke(Failure(ex))
      }
    )
  }

  def publishPending(msg: I): Unit = ()

  private def pushDownstream(message: MqttMessageWithAck): Unit = {
    push(out, message)
    backpressurePahoClient.release()
    if (manualAcks) unackedMessages.incrementAndGet()
  }

  private def failStageWith(ex: Throwable): Unit = {
    subscriptionPromise.tryFailure(ex)
    failStage(ex)

  }

  override def preStart(): Unit =
    try {
      mqttClient.connect(
        connectionSettings.asMqttConnectionOptions(),
        /* userContext */ null,
        /* callback */ new MqttActionListener {
          override def onSuccess(v: IMqttToken): Unit = onConnect.invoke(mqttClient)
          override def onFailure(asyncActionToken: IMqttToken, ex: Throwable): Unit = onConnectionLost.invoke(ex)
        }
      )
    } catch {
      case e: Throwable => failStageWith(e)
    }

  override def postStop(): Unit = {
    if (!subscriptionPromise.isCompleted) {
      subscriptionPromise
        .tryFailure(
          new IllegalStateException(
            "Cannot complete subscription because the stage is about to stop or fail"
          )
        )
    }

    try {
      log.debug(
        "Stage stopped, disconnecting client [{}] from broker [{}]",
        connectionSettings.clientId,
        connectionSettings.broker
      )

      mqttClient.disconnect(
        /* quiesceTimeout */ connectionSettings.disconnect.quiesceTimeout.toMillis,
        /* userContext */ null,
        /* callback */ new MqttActionListener {
          override def onSuccess(asyncActionToken: IMqttToken): Unit =
            mqttClient.close()

          override def onFailure(asyncActionToken: IMqttToken, ex: Throwable): Unit = {
            mqttClient.disconnectForcibly(
              /* quiesceTimeout */ 0L, // we already quiesced in `disconnect`
              /* disconnectTimeout */ connectionSettings.disconnect.timeout.toMillis,
              /* sendDisconnectPacket */ connectionSettings.disconnect.sendDisconnectPacket
            )

            mqttClient.close()
          }
        },
        /* reasonCode */ MqttReturnCode.RETURN_CODE_SUCCESS,
        /* disconnectProperties */ new MqttProperties()
      )
    } catch {
      // disconnect is "best effort"; ignore if it fails
      case _: MqttException =>
        try {
          mqttClient.close()
        } catch {
          case _: MqttException => () // do nothing
        }
    }
  }
}

/**
 * INTERNAL API
 */
@InternalApi
private[mqttv5] object MqttFlowStageLogic {
  private val SuccessfullyDone = Success(Done)

  final private case class CommitCallbackArguments(messageId: Int, qos: MqttQoS, promise: Promise[Done])
}
