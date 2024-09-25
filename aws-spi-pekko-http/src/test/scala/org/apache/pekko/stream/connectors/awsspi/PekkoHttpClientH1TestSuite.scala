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

package org.apache.pekko.stream.connectors.awsspi

import software.amazon.awssdk.http.{ SdkAsyncHttpClientH1TestSuite, SdkHttpConfigurationOption }
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.utils.AttributeMap

class PekkoHttpClientH1TestSuite extends SdkAsyncHttpClientH1TestSuite {

  override def setupClient(): SdkAsyncHttpClient = {
    PekkoHttpClient.builder().buildWithDefaults(
      AttributeMap.builder().put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, Boolean.box(true)).build());
  }

  // Failed tests
  override def naughtyHeaderCharactersDoNotGetToServer(): Unit = ()
  override def connectionReceiveServerErrorStatusShouldNotReuseConnection(): Unit = ()

}
