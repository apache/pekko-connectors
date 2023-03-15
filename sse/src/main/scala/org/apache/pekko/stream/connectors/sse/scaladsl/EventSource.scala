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

package org.apache.pekko.stream.connectors.sse
package scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.{ ActorSystem, ClassicActorSystemProvider }
import pekko.http.scaladsl.client.RequestBuilding.Get
import pekko.http.scaladsl.coding.Coders
import pekko.http.scaladsl.model.MediaTypes.`text/event-stream`
import pekko.http.scaladsl.model.headers.{ `Last-Event-ID`, Accept }
import pekko.http.scaladsl.model.sse.ServerSentEvent
import pekko.http.scaladsl.model.sse.ServerSentEvent.heartbeat
import pekko.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import pekko.http.scaladsl.unmarshalling.Unmarshal
import pekko.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling
import pekko.stream.SourceShape
import pekko.stream.scaladsl.{ Broadcast, Flow, GraphDSL, Merge, Source }

import scala.concurrent.Future
import scala.concurrent.duration.{ Duration, FiniteDuration }

/**
 * This stream processing stage establishes a continuous source of server-sent events from the given URI.
 *
 * A single source of server-sent events is obtained from the URI. Once completed, either normally or by failure, a next
 * one is obtained thereby sending a Last-Event-ID header if available. This continues in an endless cycle.
 *
 * The shape of this processing stage is a source of server-sent events; to take effect it must be connected and run.
 * Progress (including termination) is controlled by the connected flow or sink, e.g. a retry delay can be implemented
 * by streaming the materialized values of the handler via a throttle.
 *
 * {{{
 * + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
 *                                               +---------------------+
 * |                                             |       trigger       | |
 *                                               +----------o----------+
 * |                                                        |            |
 *                                            Option[String]|
 * |                                                        v            |
 *              Option[String]                   +----------o----------+
 * |            +------------------------------->o        merge        | |
 *              |                                +----------o----------+
 * |            |                                           |            |
 *              |                             Option[String]|
 * |            |                                           v            |
 *   +--------o--------+                         +----------o----------+
 * | |   lastEventId   |                         |   continuousEvents  | |
 *   +--------o--------+                         +----------o----------+
 * |            ^                                           |            |
 *              |     ServerSentEvent (including delimiters)|
 * |            |                                           v            |
 *              |                                +----------o----------+
 * |            +--------------------------------o        bcast        | |
 *              ServerSentEvent (incl. delim.)   +----------o----------+
 * |                                                        |            |
 *                    ServerSentEvent (including delimiters)|
 * |                                                        v            |
 *                                               +----------o----------+
 * |                                  +----------o       events        | |
 *                     ServerSentEvent|          +---------------------+
 * |                                  v                                  |
 *  - - - - - - - - - - - - - - - - - o - - - - - - - - - - - - - - - - -
 * }}}
 */
object EventSource {

  type EventSource = Source[ServerSentEvent, NotUsed]

  private val noEvents = Source.empty[ServerSentEvent]

  private val singleDelimiter = Source.single(heartbeat)

  /**
   * @param uri URI with absolute path, e.g. "http://myserver/events
   * @param send function to send a HTTP request
   * @param initialLastEventId initial value for Last-Event-ID header, `None` by default
   * @param retryDelay delay for retrying after completion, `0` by default
   * @param system implicit actor system (classic or new API)
   * @return continuous source of server-sent events
   */
  def apply(uri: Uri,
      send: HttpRequest => Future[HttpResponse],
      initialLastEventId: Option[String] = None,
      retryDelay: FiniteDuration = Duration.Zero)(
      implicit system: ClassicActorSystemProvider): EventSource = {
    import EventStreamUnmarshalling.fromEventsStream
    implicit val actorSystem: ActorSystem = system.classicSystem
    import actorSystem.dispatcher

    val continuousEvents = {
      def getEventSource(lastEventId: Option[String]) = {
        val request = {
          val r = Get(uri).addHeader(Accept(`text/event-stream`))
          lastEventId.foldLeft(r)((r, i) => r.addHeader(`Last-Event-ID`(i)))
        }
        send(request)
          .map(response => Coders.Gzip.decodeMessage(response))
          .flatMap(Unmarshal(_).to[EventSource])
          .fallbackTo(Future.successful(noEvents))
      }
      def recover(eventSource: EventSource) = eventSource.recoverWithRetries(1, { case _ => noEvents })
      def delimit(eventSource: EventSource) = eventSource.concat(singleDelimiter)
      Flow[Option[String]]
        .mapAsync(1)(getEventSource)
        .flatMapConcat((recover _).andThen(delimit))
    }

    val lastEventId =
      Flow[ServerSentEvent]
        .prepend(Source.single(heartbeat)) // to make sliding and collect-matching work
        .sliding(2)
        .collect { case Seq(last, event) if event == ServerSentEvent.heartbeat => last }
        .scan(initialLastEventId)((prev, current) => current.id.orElse(prev))
        .drop(1)

    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val trigger = builder.add(Source.single(initialLastEventId))
      val merge = builder.add(Merge[Option[String]](2))
      val bcast = builder.add(Broadcast[ServerSentEvent](2, eagerCancel = true))
      val events = builder.add(Flow[ServerSentEvent].filter(_ != heartbeat))
      val delay = builder.add(Flow[Option[String]].delay(retryDelay))
      // format: OFF
      trigger ~> merge ~>   continuousEvents   ~> bcast ~> events
                 merge <~ delay <~ lastEventId <~ bcast
      // format: ON
      SourceShape(events.out)
    })
  }
}
