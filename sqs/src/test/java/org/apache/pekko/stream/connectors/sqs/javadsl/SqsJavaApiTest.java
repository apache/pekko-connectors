/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.sqs.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import org.apache.pekko.stream.connectors.sqs.MessageAttributeName;
import org.apache.pekko.stream.connectors.sqs.SqsAckGroupedSettings;
import org.apache.pekko.stream.connectors.sqs.SqsPublishGroupedSettings;
import org.apache.pekko.stream.connectors.sqs.SqsPublishResult;
import org.apache.pekko.stream.connectors.sqs.SqsSourceSettings;
import org.apache.pekko.stream.connectors.sqs.testkit.MessageFactory;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/** Exercises the Java API of the SQS settings and results without SQS. */
public class SqsJavaApiTest {

  @Test
  public void sourceSettingsGetters() {
    MessageAttributeName attribute = MessageAttributeName.create("attribute");
    SqsSourceSettings settings =
        SqsSourceSettings.create()
            .withVisibilityTimeout(Duration.ofSeconds(10))
            .withMessageAttributes(Collections.singletonList(attribute));
    assertEquals(Optional.of(Duration.ofSeconds(10)), settings.getVisibilityTimeout());
    assertEquals(Collections.singletonList(attribute), settings.getMessageAttributeNames());
    assertEquals(0, SqsSourceSettings.create().getAttributeNames().size());
  }

  @Test
  public void groupedSettingsMaxBatchWait() {
    assertEquals(
        Duration.ofSeconds(2),
        SqsPublishGroupedSettings.create(5, Duration.ofSeconds(2), 1).getMaxBatchWait());
    assertEquals(
        Duration.ofSeconds(3),
        SqsAckGroupedSettings.create(5, Duration.ofSeconds(3), 1).getMaxBatchWait());
  }

  @Test
  public void resultGettersAreTyped() {
    SendMessageRequest request = SendMessageRequest.builder().queueUrl("url").build();
    SendMessageResponse response = SendMessageResponse.builder().messageId("id").build();
    SqsPublishResult result = MessageFactory.createSqsPublishResult(request, response);

    SendMessageResponse typed = result.getResult();
    assertSame(response, typed);
    assertSame(request, result.getRequest());
    assertEquals(result.responseMetadata(), result.getResponseMetadata());
  }
}
