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

package org.apache.pekko.stream.connectors.google.javadsl;

import com.typesafe.config.Config;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.unmarshalling.Unmarshaller;
import org.apache.pekko.stream.connectors.google.ForwardProxy;
import org.apache.pekko.stream.connectors.google.GoogleSettings;
import org.apache.pekko.stream.connectors.google.RequestSettings;
import org.apache.pekko.stream.connectors.google.auth.Credentials;

/** Compile-only checks for the Google common Java API. */
public class GoogleJavaApiTest {

  @SuppressWarnings("unused")
  private static void apiCompiles(
      HttpRequest request,
      Unmarshaller<HttpResponse, String> unmarshaller,
      GoogleSettings settings,
      ClassicActorSystemProvider system,
      Config config,
      RequestSettings requestSettings) {
    CompletionStage<String> result = Google.singleRequest(request, unmarshaller, settings, system);
    Credentials credentials = Credentials.create(config, system);
    Optional<ForwardProxy> forwardProxy = requestSettings.getForwardProxyOptional();
  }
}
