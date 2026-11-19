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

package org.apache.pekko.stream.connectors.sse.javadsl;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.Uri;
import org.apache.pekko.http.javadsl.model.sse.ServerSentEvent;
import org.apache.pekko.stream.javadsl.Source;

/** Compile-only checks for the server-sent events Java API. */
public class SseJavaApiTest {

  @SuppressWarnings("unused")
  private static void eventSourceCompiles(
      ActorSystem system, Function<HttpRequest, CompletionStage<HttpResponse>> send) {
    Uri uri = Uri.create("http://localhost/events");
    Source<ServerSentEvent, NotUsed> withRetryDelay =
        EventSource.create(uri, send, Optional.empty(), Duration.ofSeconds(1), system);
    Source<ServerSentEvent, NotUsed> minimal = EventSource.create(uri, send, system);
  }
}
