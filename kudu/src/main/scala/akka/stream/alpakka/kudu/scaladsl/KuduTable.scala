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

package akka.stream.alpakka.kudu.scaladsl

import akka.stream.{ Attributes, Materializer }
import akka.stream.alpakka.kudu.{ KuduAttributes, KuduClientExt, KuduTableSettings }
import akka.stream.alpakka.kudu.impl.KuduFlowStage
import akka.stream.scaladsl.{ Flow, Keep, Sink }
import akka.{ Done, NotUsed }

import scala.concurrent.Future

/**
 * Scala API
 */
object KuduTable {

  /**
   * Create a Sink writing elements to a Kudu table.
   */
  def sink[A](settings: KuduTableSettings[A]): Sink[A, Future[Done]] =
    flow(settings).toMat(Sink.ignore)(Keep.right)

  /**
   * Create a Flow writing elements to a Kudu table.
   */
  def flow[A](settings: KuduTableSettings[A]): Flow[A, A, NotUsed] =
    Flow
      .fromMaterializer { (mat, attr) =>
        Flow.fromGraph(new KuduFlowStage[A](settings, client(mat, attr)))
      }
      .mapMaterializedValue(_ => NotUsed)

  private def client(mat: Materializer, attr: Attributes) =
    attr
      .get[KuduAttributes.Client]
      .map(_.client)
      .getOrElse(KuduClientExt(mat.system).client)
}
