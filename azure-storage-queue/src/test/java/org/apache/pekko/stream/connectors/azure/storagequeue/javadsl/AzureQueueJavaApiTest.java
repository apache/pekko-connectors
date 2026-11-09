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

package org.apache.pekko.stream.connectors.azure.storagequeue.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.Test;

/** Exercises the Java API additions of the Azure Storage Queue connector. */
public class AzureQueueJavaApiTest {

  @Test
  public void sinkFromConsumer() throws Exception {
    ActorSystem system = ActorSystem.create();
    try {
      List<Integer> seen = new CopyOnWriteArrayList<>();
      Source.from(Arrays.asList(1, 2, 3))
          .runWith(AzureQueueSink.<Integer>fromConsumer(seen::add), system)
          .toCompletableFuture()
          .get(3, TimeUnit.SECONDS);
      assertEquals(Arrays.asList(1, 2, 3), seen);
    } finally {
      TestKit.shutdownActorSystem(system);
    }
  }
}
