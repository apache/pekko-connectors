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

package org.apache.pekko.stream.connectors.google.firebase.fcm.v1.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.apache.pekko.stream.connectors.google.firebase.fcm.v1.models.*;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the FCM models without FCM. */
public class FcmJavaApiTest {

  private static final Map<String, String> DATA = Collections.singletonMap("k", "v");

  @Test
  public void withersTakeJavaCollections() {
    assertEquals("v", FcmNotification.fromJava().withData(DATA).data().get().apply("k"));
    assertEquals("v", AndroidConfig.fromJava().withData(DATA).data().get().apply("k"));
    assertEquals("v", ApnsConfig.fromJava().withHeaders(DATA).headers().get().apply("k"));
    WebPushConfig webPush = WebPushConfig.fromJava().withHeaders(DATA).withData(DATA);
    assertEquals("v", webPush.data().get().apply("k"));
    assertEquals(
        "a",
        AndroidNotification.fromJava()
            .withBodyLocArgs(Arrays.asList("a"))
            .withTitleLocArgs(Arrays.asList("b"))
            .withVibrateTimings(Arrays.asList("c"))
            .body_loc_args()
            .get()
            .head());
  }

  @Test
  public void enumValuesFromJava() {
    AndroidConfig config = AndroidConfig.fromJava().withPriority(High.getInstance());
    assertEquals(High.getInstance(), config.priority().get());
    assertEquals(
        Secret.getInstance(),
        AndroidNotification.fromJava().withVisibility(Secret.getInstance()).visibility().get());
    assertEquals(
        PriorityMax.getInstance(),
        AndroidNotification.fromJava()
            .withNotificationPriority(PriorityMax.getInstance())
            .notification_priority()
            .get());
  }

  @Test
  public void conditionBuilderWithoutSymbolicOperators() {
    Condition.ConditionBuilder builder =
        new Condition.Topic("a").and(new Condition.Topic("b")).or(new Condition.Topic("c")).not();
    assertEquals(
        "!((('a' in topics && 'b' in topics) || 'c' in topics))", builder.toConditionText());
  }
}
