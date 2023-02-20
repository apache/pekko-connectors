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

package akka.stream.alpakka.mongodb.javadsl

import akka.NotUsed
import akka.stream.javadsl.Source
import org.reactivestreams.Publisher

object MongoSource {

  def create[T](query: Publisher[T]): Source[T, NotUsed] =
    Source.fromPublisher(query)

}
