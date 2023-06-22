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

package org.apache.pekko.stream.connectors.sqs.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.sqs.SqsSourceSettings
import pekko.stream.javadsl.Source
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message

/**
 * Java API to create SQS sources.
 */
object SqsSource {

  /**
   * creates a [[pekko.stream.javadsl.Source Source]] for a SQS queue using [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def create(queueUrl: String, settings: SqsSourceSettings, sqs: SqsAsyncClient): Source[Message, NotUsed] =
    pekko.stream.connectors.sqs.scaladsl.SqsSource(queueUrl, settings)(sqs).asJava

}
