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

package org.apache.pekko.stream.connectors.geode.scaladsl

import org.apache.pekko
import pekko.stream.connectors.geode.impl._
import pekko.stream.connectors.geode.impl.pdx.{ PdxDecoder, PdxEncoder, ShapelessPdxSerializer }
import pekko.stream.connectors.geode.impl.stage.{ GeodeContinuousSourceStage, GeodeFiniteSourceStage, GeodeFlowStage }
import pekko.stream.connectors.geode.{ GeodeSettings, PekkoPdxSerializer, RegionSettings }
import pekko.stream.scaladsl.{ Flow, Keep, Sink, Source }
import pekko.{ Done, NotUsed }
import org.apache.geode.cache.client.ClientCacheFactory

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Scala API: Geode client without server event subscription.
 */
class Geode(settings: GeodeSettings) extends GeodeCache(settings) {

  /**
   * This method is overloaded by [[PoolSubscription]] to provide server event subscriptions.
   */
  override protected def configure(factory: ClientCacheFactory): ClientCacheFactory =
    factory.addPoolLocator(settings.hostname, settings.port)

  def query[V <: AnyRef](query: String, serializer: PekkoPdxSerializer[V]): Source[V, Future[Done]] = {

    registerPDXSerializer(serializer, serializer.clazz)

    Source.fromGraph(new GeodeFiniteSourceStage[V](cache, query))
  }

  def flow[K, V <: AnyRef](settings: RegionSettings[K, V], serializer: PekkoPdxSerializer[V]): Flow[V, V, NotUsed] = {

    registerPDXSerializer(serializer, serializer.clazz)

    Flow.fromGraph(new GeodeFlowStage[K, V](cache, settings))
  }

  def sink[K, V <: AnyRef](settings: RegionSettings[K, V], serializer: PekkoPdxSerializer[V]): Sink[V, Future[Done]] =
    Flow[V].via(flow(settings, serializer)).toMat(Sink.ignore)(Keep.right)

  /**
   * Shapeless powered implicit serializer.
   */
  def query[V <: AnyRef](
      query: String)(implicit tag: ClassTag[V], enc: PdxEncoder[V], dec: PdxDecoder[V]): Source[V, Future[Done]] = {

    registerPDXSerializer(new ShapelessPdxSerializer[V](enc, dec), tag.runtimeClass)

    Source.fromGraph(new GeodeFiniteSourceStage[V](cache, query))
  }

  /**
   * Shapeless powered implicit serializer.
   */
  def flow[K, V <: AnyRef](
      settings: RegionSettings[K, V])(
      implicit tag: ClassTag[V], enc: PdxEncoder[V], dec: PdxDecoder[V]): Flow[V, V, NotUsed] = {

    registerPDXSerializer(new ShapelessPdxSerializer[V](enc, dec), tag.runtimeClass)

    Flow.fromGraph(new GeodeFlowStage[K, V](cache, settings))
  }

  /**
   * Shapeless powered implicit serializer.
   */
  def sink[K, V <: AnyRef](
      settings: RegionSettings[K, V])(
      implicit tag: ClassTag[V], enc: PdxEncoder[V], dec: PdxDecoder[V]): Sink[V, Future[Done]] =
    Flow[V].via(flow(settings)).toMat(Sink.ignore)(Keep.right)

}

trait PoolSubscription extends Geode {

  /**
   * Pool subscription is mandatory for continuous query.
   */
  final override protected def configure(factory: ClientCacheFactory) =
    super.configure(factory).setPoolSubscriptionEnabled(true)

  def continuousQuery[V <: AnyRef](queryName: Symbol,
      query: String,
      serializer: PekkoPdxSerializer[V]): Source[V, Future[Done]] = {

    registerPDXSerializer(serializer, serializer.clazz)

    Source.fromGraph(new GeodeContinuousSourceStage[V](cache, queryName.name, query))
  }

  /**
   * Shapeless powered implicit serializer.
   */
  def continuousQuery[V <: AnyRef](
      queryName: Symbol,
      query: String)(implicit tag: ClassTag[V], enc: PdxEncoder[V], dec: PdxDecoder[V]): Source[V, Future[Done]] = {

    registerPDXSerializer(new ShapelessPdxSerializer[V](enc, dec), tag.runtimeClass)

    Source.fromGraph(new GeodeContinuousSourceStage[V](cache, queryName.name, query))
  }

  def closeContinuousQuery(queryName: Symbol) =
    for {
      qs <- Option(cache.getQueryService())
      query <- Option(qs.getCq(queryName.name))
    } yield query.close()

}
