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

package org.apache.pekko.stream.connectors.huawei.pushkit.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.apache.pekko.stream.connectors.huawei.pushkit.ForwardProxy;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.AndroidNotification;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.Condition;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.Tokens;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.WebConfig;
import org.apache.pekko.stream.connectors.huawei.pushkit.models.WebNotification;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the Huawei Push Kit models without a server. */
public class HmsPushKitJavaApiTest {

  @Test
  public void tokensFromJavaList() {
    Tokens tokens = Tokens.create(Arrays.asList("token1", "token2"));
    assertEquals("token1", tokens.token().head());
  }

  @Test
  public void conditionBuilderWithoutSymbolicOperators() {
    Condition.ConditionBuilder builder =
        new Condition.Topic("a").and(new Condition.Topic("b")).or(new Condition.Topic("c")).not();
    assertEquals(
        "!((('a' in topics && 'b' in topics) || 'c' in topics))", builder.toConditionText());
  }

  @Test
  public void withersTakeJavaCollections() {
    AndroidNotification android =
        AndroidNotification.fromJava()
            .withBodyLocArgs(Arrays.asList("a"))
            .withTitleLocArgs(Arrays.asList("b"))
            .withVibrateConfig(Arrays.asList("c"))
            .withInboxContent(Arrays.asList("d"));
    assertEquals("a", android.body_loc_args().get().head());

    assertEquals(
        "1",
        WebConfig.fromJava()
            .withHeaders(Collections.singletonMap("h", "1"))
            .headers()
            .get()
            .apply("h"));
    assertEquals(
        3, WebNotification.fromJava().withVibrate(Arrays.asList(1, 2, 3)).vibrate().get().size());
  }

  @Test
  public void forwardProxyWithOptional() {
    ForwardProxy proxy = ForwardProxy.create("host", 8080, Optional.empty(), Optional.empty());
    assertEquals(Optional.empty(), proxy.getCredentialsOptional());
    assertEquals(Optional.empty(), proxy.getForwardProxyTrustPemOptional());
  }
}
