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

package org.apache.pekko.stream.connectors.google.util

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.FlowShape
import pekko.stream.scaladsl.{ Flow, GraphDSL, Keep, Merge, Partition }

@InternalApi
private[google] object EitherFlow {

  def apply[LeftIn, LeftOut, LeftMat, RightIn, RightOut, RightMat](
      leftFlow: Flow[LeftIn, LeftOut, LeftMat],
      rightFlow: Flow[RightIn, RightOut, RightMat])
      : Flow[Either[LeftIn, RightIn], Either[LeftOut, RightOut], (LeftMat, RightMat)] =
    Flow.fromGraph(
      GraphDSL.createGraph(leftFlow, rightFlow)(Keep.both) { implicit b => (leftFlow, rightFlow) =>
        import GraphDSL.Implicits._
        val in = b.add(Partition[Either[LeftIn, RightIn]](2, x => if (x.isRight) 1 else 0))
        val out = b.add(Merge[Either[LeftOut, RightOut]](2))
        in ~> Flow[Either[LeftIn, RightIn]].map(_.swap.toOption.get) ~> leftFlow ~> Flow[LeftOut].map(Left(_)) ~> out
        in ~> Flow[Either[LeftIn, RightIn]].map(_.toOption.get)      ~> rightFlow ~> Flow[RightOut]
          .map(
            Right(_)) ~> out
        FlowShape(in.in, out.out)
      })

}
